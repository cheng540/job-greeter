const { spawn } = require('child_process');
const fs = require('fs');

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
const DIR = 'E:/WorkBuddy_work/BOSS话术助手';
const proc = spawn(EDGE, [
  '--headless=new', '--disable-gpu', '--no-proxy-server', '--no-first-run',
  '--disable-features=msEdgeFirstRunExperience', '--remote-debugging-port=9333',
  '--user-data-dir=' + DIR + '/.edgedbg', 'about:blank'
], { stdio: 'ignore' });

const sleep = ms => new Promise(r => setTimeout(r, ms));

const JD = `Java开发工程师（实习）
6-8K
武汉 · 洪山区 · 经验不限 · 本科
武汉青云智联科技有限公司
岗位职责：
1、参与核心业务系统的后端开发，基于 SpringBoot + MyBatis-Plus 架构；
2、负责接口设计与数据库表结构设计，编写 RESTful 接口；
3、配合前端完成联调，参与需求评审。
任职要求：
1、熟悉 Java 基础，了解 JVM、多线程；
2、熟悉 MySQL 数据库，会写 SQL；
3、了解 Redis 缓存、消息队列者优先；
4、2027 届本科及以上，可实习 6 个月以上。`;

(async () => {
  await sleep(3000);
  const list = await (await fetch('http://127.0.0.1:9333/json')).json();
  const page = list.find(t => t.type === 'page');
  if (!page) { console.log('找不到页面 target'); proc.kill(); return; }

  const ws = new WebSocket(page.webSocketDebuggerUrl);
  let id = 0;
  const pending = new Map();
  ws.onmessage = e => {
    const m = JSON.parse(e.data);
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); }
  };
  await new Promise(r => (ws.onopen = r));
  const send = (method, params) => new Promise(res => {
    const i = ++id; pending.set(i, res);
    ws.send(JSON.stringify({ id: i, method, params: params || {} }));
  });

  await send('Page.enable');
  await send('Runtime.enable');
  await send('Emulation.setDeviceMetricsOverride', {
    width: 390, height: 844, deviceScaleFactor: 2, mobile: true
  });
  await send('Page.navigate', { url: 'http://124.220.191.183:8000/talk.html' });
  await sleep(2600);

  const ev = async expr => {
    const r = await send('Runtime.evaluate', { expression: expr, returnByValue: true });
    return r.result && r.result.result ? r.result.result.value : null;
  };

  console.log('标题:', await ev('document.title'));
  console.log('视口:', await ev('innerWidth + "x" + innerHeight'));
  console.log('溢出:', await ev('document.documentElement.scrollWidth > innerWidth ? "有" : "无"'));

  const shot1 = await send('Page.captureScreenshot', { format: 'png' });
  fs.writeFileSync(DIR + '/preview-empty.png', Buffer.from(shot1.result.data, 'base64'));

  await ev(`(function(){
    var t = document.getElementById('jd');
    t.value = ${JSON.stringify(JD)};
    t.dispatchEvent(new Event('input'));
  })()`);
  await sleep(400);
  console.log('识别标签:', await ev('document.getElementById("chips").textContent'));

  await ev('document.getElementById("gen").click()');
  await sleep(800);

  const out = await ev('document.getElementById("out").textContent');
  console.log('结果字数:', out ? out.length : 0);
  console.log('结果卡片可见:', await ev('!document.getElementById("resultCard").hidden'));
  console.log('--- 生成内容 ---');
  console.log(out);

  const shot2 = await send('Page.captureScreenshot', { format: 'png' });
  fs.writeFileSync(DIR + '/preview-result.png', Buffer.from(shot2.result.data, 'base64'));

  await send('Page.navigate', { url: 'about:blank' });
  ws.close();
  proc.kill();
  await sleep(300);
  console.log('\n截图已保存');
})();
