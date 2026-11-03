const fs = require('fs');
const vm = require('vm');

const html = fs.readFileSync('E:/WorkBuddy_work/BOSS话术助手/talk.html', 'utf8');
const m = html.match(/<script>([\s\S]*?)<\/script>/);
let code = m[1];
const hook = '\n globalThis.__T = { parse: parse, build: build };\n})();';
code = code.replace(/\}\) \(\);\s*$/, hook).replace(/\}\)\(\);\s*$/, hook);

function el() {
  return {
    value: '', textContent: '', innerHTML: '', hidden: false, checked: false,
    style: {}, classList: { add() {}, remove() {}, contains() { return false; } },
    addEventListener() {}, appendChild() {}, focus() {}, scrollIntoView() {},
    select() {}, setAttribute() {}, getAttribute() { return null; }
  };
}
const document = {
  getElementById: () => el(), createElement: () => el(), body: el(),
  querySelectorAll: () => [], addEventListener() {}
};
const ctx = {
  document,
  localStorage: { getItem: () => null, setItem() {} },
  navigator: { clipboard: null },
  window: {}, setTimeout, clearTimeout, console, JSON, Math, Date, RegExp, String, Array, Object
};
vm.createContext(ctx);
vm.runInContext(code, ctx);

const samples = [
  ['Java 后端开发（校招实习）',
`Java开发工程师（实习）
薪资：6-8K
武汉 · 洪山区 · 经验不限 · 本科
武汉某某科技有限公司
岗位职责：
1、参与公司核心业务系统的后端开发，基于 SpringBoot + MyBatis-Plus 架构；
2、负责接口设计与数据库表结构设计，编写 RESTful 接口；
3、配合前端完成联调，参与需求评审。
任职要求：
1、熟悉 Java 基础，了解 JVM、多线程；
2、熟悉 MySQL 数据库，会写 SQL；
3、了解 Redis 缓存、消息队列者优先；
4、2027 届本科及以上，可实习 6 个月以上。`],

  ['前端开发（Vue）',
`前端开发工程师
10-15K
武汉 · 本科
某某网络科技有限公司
职位描述：
1、负责公司 Web 端产品的前端开发，使用 Vue3 + Element Plus；
2、参与移动端 H5 页面开发，保证多端适配；
3、与后端对接接口，完成数据可视化页面（ECharts）；
4、要求熟悉 HTML/CSS/JavaScript，有组件化开发经验。`],

  ['软件测试实习',
`软件测试实习生
4-6K
武汉 · 经验不限 · 本科
某某信息技术有限公司
岗位职责：
1、根据需求文档编写测试用例，执行功能测试与回归测试；
2、使用 Postman 进行接口测试；
3、提报并跟踪缺陷，推动问题闭环；
4、参与自动化测试脚本编写（Selenium/Pytest 优先）。`],

  ['AI 应用开发',
`AI应用开发工程师
15-25K
武汉 · 硕士优先
某某智能科技有限公司
职责：
1、负责大模型应用落地，基于 LLM 构建企业知识库问答系统（RAG）；
2、负责 Prompt 设计与优化，提升模型输出准确率；
3、对接向量数据库，完成文档切分、embedding 与检索；
4、了解 Function Calling、多模态能力者优先。`],

  ['Agent 开发',
`AI Agent 开发工程师
20-30K
武汉
某某数据科技有限公司
岗位职责：
1、负责智能体 Agent 的架构设计与开发，基于 Function Calling 实现工具调用；
2、设计多步骤任务规划与记忆机制，提升任务完成率；
3、优化工具调用准确率，降低模型幻觉；
4、有 MCP、工作流编排经验者优先。`],

  ['无公司信息的短文本',
`后端开发实习生
熟悉 Node.js / Express，了解 WebSocket 实时通信，会 Linux 部署。`]
];

let pass = 0;
samples.forEach(function (s, i) {
  const p = ctx.__T.parse(s[1]);
  const expect = ['java', 'front', 'test', 'ai', 'agent', 'back'][i];
  const ok = p.type === expect;
  if (ok) pass++;
  console.log('--- [' + (i + 1) + '] ' + s[0] + ' ---');
  console.log('  判定: ' + p.type + (ok ? '  ✓' : '  ✗ 期望 ' + expect));
  console.log('  公司: ' + (p.company || '(未识别)') + ' | 岗位: ' + (p.title || '(未识别)') + ' | 薪资: ' + (p.salary || '(未识别)'));
});
console.log('\n识别准确: ' + pass + '/' + samples.length);

console.log('\n===== 生成示例（Java 岗，seed=0）=====');
const p0 = ctx.__T.parse(samples[0][1]);
console.log(ctx.__T.build(p0, 'java', 0));
console.log('\n===== 生成示例（Agent 岗，seed=1）=====');
const p4 = ctx.__T.parse(samples[4][1]);
console.log(ctx.__T.build(p4, 'agent', 1));
