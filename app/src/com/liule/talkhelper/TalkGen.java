package com.liule.talkhelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TalkGen {

    static final String[] KEYS = {"java", "front", "back", "test", "ai", "agent"};
    static final String[] NAMES = {"Java 开发", "前端开发", "后端开发", "软件测试", "AI 应用开发", "Agent 开发"};

    static final String[][] KW = {
        {"java", "spring", "springboot", "spring boot", "mybatis", "jvm", "mvc", "微服务", "dubbo", "hibernate", "ssm", "jpa"},
        {"前端", "vue", "react", "html", "css", "javascript", "typescript", "uniapp", "小程序", "h5", "webpack", "vite", "element", "antd"},
        {"后端", "服务端", "node", "express", "golang", "go语言", "python", "redis", "mysql", "数据库", "并发", "接口设计", "websocket", "nginx", "linux"},
        {"测试", "用例", "junit", "自动化测试", "selenium", "postman", "缺陷", "回归", "pytest", "appium", "质量", "黑盒", "白盒"},
        {"大模型", "llm", "rag", "prompt", "提示词", "向量", "微调", "langchain", "知识库", "多模态", "ocr", "embedding", "检索增强", "深度学习", "机器学习", "图像", "计算机视觉", "cnn", "卷积", "lstm", "pytorch", "算法实习", "模型训练", "数据清洗", "数据分析", "数据挖掘", "机器感知"},
        {"agent", "智能体", "function calling", "工具调用", "工作流", "mcp", "tool", "规划", "记忆", "自主"}
    };

    static final String[][] EV = {
        {
            "做过完整的后台管理系统：从数据库表设计、接口开发、权限控制到前端联调都是自己完成的，交付时附源码和部署文档",
            "做过订单类业务，处理过订单状态流转、支付安全校验和价格一致性这类容易出问题的点",
            "同类管理系统交付过多套，每套都附源码、部署文档和使用教程，接手的人能直接跑起来",
            "前端 Vue3 + Element Plus 也能自己写，不用等前端排期"
        },
        {
            "做过三端项目：用户端和管理端独立工程、后端接口分离，两端分开构建、职责清晰",
            "做过手机优先的 Web 应用：宫格导航、实时联机房间、内容聚合页都实现过，线上有真实用户在用",
            "用 ECharts 做过趋势图和行情看板，数据可视化的常见坑基本踩过",
            "接口对接、切图还原、移动端适配都能独立完成，不用前后端互相等"
        },
        {
            "用 Node.js 做过 HTTP 与 WebSocket 同端口单进程的实时联机服务，支持房间邀请和 AI 补位",
            "对战类玩法用服务端权威判定，客户端改数据不生效，防住了常见作弊手段",
            "Spring Boot + MyBatis-Plus + JWT 的管理后台也写过，配 Vue3 前端",
            "服务器自己运维：Linux、nginx、systemd、Docker 部署都熟，出问题能自己查日志"
        },
        {
            "单元测试按正常 / 边界 / null 三类输入分别覆盖，断言写全",
            "接口测试做过断言脚本加数据驱动批量运行，附完整测试记录表",
            "网页项目用 Playwright 写回归用例并全部跑通",
            "自己项目里排查修复过硬编码、注入、性能隐患这类缺陷，清楚缺陷一般藏在哪"
        },
        {
            "做过基于 Function Calling 的对话助手：封装检索、解析、识别、合成等一批工具由模型自主调度",
            "RAG 做过两套：轻量版用分词加 BM25 不依赖向量库，完整版用向量库加文档入库和片段溯源",
            "服务部署在云服务器上长期运行，不是本地跑个 demo",
            "做过知识库问答、微博热搜情绪分析（Kafka 生产者 + Spark 流式窗口统计 + ECharts 可视化）这类完整案例",
            "深度学习用过 PyTorch：卷积神经网络做手写数字识别，两卷积两池化两全连接，交叉熵加 SGD，画每个 epoch 的损失曲线，还把手写拍照的图预处理后交给训练好的模型预测",
            "LSTM 时间序列做过完整对比实验：277 年的太阳黑子月均序列，滑动窗口构样本，两层 LSTM 加早停，跟持续性和线性外推两个基线比 MSE/RMSE/MAE，外加网络容量和学习率的超参对照",
            "数据清洗做过真实业务数据：带货币符号和千分位的金额字段转数值、缺失值按字段分别补齐、异常值单独导出待核查台账，最后从脏数据产出分析简报"
        },
        {
            "做过连续运行 30 天以上的无人值守 Agent：定时抓数据、算指标、生成日报并自动推送，全程无人工干预",
            "做了防幻觉机制：模型只能通过工具取数、数字自动交叉校验、每条结论附依据",
            "告警分级：紧急即时推、普通汇总、连续确认才推送，解决什么时候该打扰人的问题",
            "规则引擎管事实、大模型管解读的分工方式，踩过坑调过几轮"
        }
    };

    static final String[][] EVK = {
        {"spring", "mybatis", "jwt", "工单", "管理系统", "erp", "oa", "权限"},
        {"order", "支付", "商城", "电商", "外卖", "交易", "订单"},
        {"交付", "文档", "维护", "实施", "部署"},
        {"vue", "element", "全栈", "前后端"},
        {"vue", "管理后台", "多端", "组件", "工程化"},
        {"移动端", "h5", "响应式", "适配", "小程序"},
        {"echarts", "可视化", "图表", "数据大屏", "canvas"},
        {"接口", "联调", "还原", "适配"},
        {"node", "websocket", "实时", "长连接", "socket"},
        {"安全", "校验", "并发", "一致性", "防作弊"},
        {"java", "spring", "mysql", "后台"},
        {"linux", "部署", "运维", "docker", "nginx", "服务器"},
        {"junit", "单元测试", "白盒", "java", "断言"},
        {"postman", "接口测试", "api", "自动化"},
        {"playwright", "自动化测试", "ui", "selenium", "回归"},
        {"缺陷", "bug", "排查", "定位", "安全"},
        {"function calling", "工具", "多模态", "ocr", "语音", "插件"},
        {"rag", "向量", "知识库", "检索", "embedding", "文档"},
        {"部署", "上线", "工程化", "落地"},
        {"知识库", "数据", "流式", "实时", "行业"},
        {"自动化", "无人值守", "定时", "任务", "流程", "落地"},
        {"幻觉", "准确", "可靠", "校验", "评估", "function calling"},
        {"告警", "推送", "通知", "策略", "工作流"},
        {"架构", "设计", "工程", "混合", "规则"}
    };

    static final String[] CLOSERS = {
        "方便的话，我先发一份简历和项目源码给您过目？",
        "发您一份作品集看看？",
        "可以的话我把简历和项目细节发您？",
        "有兴趣的话我发您简历和项目源码？"
    };

    private static final Pattern P_COMPANY_FULL = Pattern.compile(
        "([\\u4e00-\\u9fa5A-Za-z0-9（）()·]{2,20}(?:股份有限公司|有限责任公司|有限公司))");
    private static final Pattern P_COMPANY_SHORT = Pattern.compile(
        "([\\u4e00-\\u9fa5A-Za-z0-9（）()·]{2,16}(?:科技|网络|信息技术|软件|集团|研究院|工作室|数码|数据))");
    private static final Pattern P_SALARY = Pattern.compile(
        "(\\d{1,3}(?:\\.\\d)?)\\s*[-~—至]\\s*(\\d{1,3}(?:\\.\\d)?)\\s*[kK千]");
    private static final Pattern P_TITLE = Pattern.compile(
        "工程师|开发|测试|实习|前端|后端|算法|Agent|产品|专员|助理", Pattern.CASE_INSENSITIVE);

    static final class Result {
        String type = "java";
        String company = "";
        String title = "";
        String salary = "";
        String raw = "";
    }

    private TalkGen() {}

    static Result parse(String text) {
        Result r = new Result();
        r.raw = text == null ? "" : text;
        String low = r.raw.toLowerCase();

        int[] score = new int[KEYS.length];
        for (int t = 0; t < KW.length; t++) {
            int s = 0;
            for (String k : KW[t]) {
                if (low.contains(k)) s += (k.length() > 4 ? 2 : 1);
            }
            score[t] = s;
        }
        if (low.contains("智能体") || low.contains("function calling") || low.contains("工具调用")
                || low.contains("mcp") || low.contains("agent")) {
            score[5] += 3;
        }
        if (score[3] > 0 && score[3] < score[0]) score[3] = Math.max(0, score[3] - 2);

        int best = 0;
        for (int i = 1; i < score.length; i++) if (score[i] > score[best]) best = i;
        r.type = KEYS[best];

        Matcher m = P_COMPANY_FULL.matcher(r.raw);
        if (m.find()) {
            r.company = m.group(1);
        } else {
            m = P_COMPANY_SHORT.matcher(r.raw);
            if (m.find()) r.company = m.group(1);
        }

        String[] lines = r.raw.split("\n");
        for (int i = 0; i < lines.length && i < 40; i++) {
            String ln = lines[i].trim();
            if (ln.length() >= 3 && ln.length() <= 24 && P_TITLE.matcher(ln).find()
                    && !Character.isDigit(ln.charAt(0))) {
                r.title = ln;
                break;
            }
        }

        m = P_SALARY.matcher(r.raw);
        if (m.find()) r.salary = m.group(1) + "-" + m.group(2) + "K";
        return r;
    }

    private static int typeIndex(String key) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return i;
        return 0;
    }

    static String typeName(String key) {
        return NAMES[typeIndex(key)];
    }

    static String build(Result r, String typeKey, int seed) {
        return build(r, typeKey, seed, "");
    }

    /**
     * @param resume 用户配置的简历文本，用来提取学校、届别、交付量、套数、常用工具。
     *               提取不到就用通用说法，绝不编数字。
     */
    static String build(Result r, String typeKey, int seed, String resume) {
        int ti = typeIndex(typeKey);
        String[] pool = EV[ti];
        String low = r.raw.toLowerCase();
        String cv = resume == null ? "" : resume;

        int base = 0;
        for (int i = 0; i < ti; i++) base += EV[i].length;

        List<Integer> order = new ArrayList<Integer>();
        List<Integer> hits = new ArrayList<Integer>();
        for (int i = 0; i < pool.length; i++) {
            int h = 0;
            String[] ks = EVK[base + i];
            for (String k : ks) if (low.contains(k)) h++;
            order.add(i);
            hits.add(h);
        }
        final List<Integer> fh = hits;
        final int fpool = pool.length;
        final int fseed = seed;
        java.util.Collections.sort(order, new java.util.Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                int d = fh.get(b) - fh.get(a);
                if (d != 0) return d;
                return ((a + fseed) % fpool) - ((b + fseed) % fpool);
            }
        });

        /* 简历里有没有交付量，决定后面塞一条还是两条证据（有量就只留一条，免得太长） */
        String vol = grab(cv, "(\\d{2,3})\\s*\\+?\\s*(?:单|项|个)");
        String sets = grab(cv, "(\\d{1,2})\\s*\\+?\\s*套");
        int evLimit = (vol != null || sets != null) ? 1 : 2;

        LinkedHashSet<Integer> picked = new LinkedHashSet<Integer>();
        if (ti == 5) picked.add(0);              // Agent 岗固定带上「无人值守」那条
        for (Integer idx : order) {
            if (picked.size() >= evLimit) break;
            picked.add(idx);
        }

        StringBuilder ev = new StringBuilder();
        for (Integer idx : picked) {
            if (ev.length() > 0) ev.append("。");
            ev.append(pool[idx]);
        }

        StringBuilder out = new StringBuilder();

        /* 第一段：身份。学校/届别从简历里取，取不到用通用说法 */
        String school = grab(cv, "([\\u4e00-\\u9fa5]{2,12}(?:大学|学院))");
        String year = grab(cv, "(20\\d{2})\\s*届");
        if (school == null && year == null) {
            out.append("您好，我是应届生，现在可以到岗。");
        } else {
            out.append("您好，我是");
            out.append(school != null ? school : "计算机相关专业");
            if (year != null) out.append(" ").append(year);
            out.append("的应届生，现在可以到岗。");
        }

        /* 第二段：交付体量。只有简历里真的找到数字才写 */
        if (vol != null) {
            out.append("\n\n课余接定制开发，前后交付过 ").append(vol).append(" 多单");
            if (sets != null) out.append("，其中同类系统做过 ").append(sets).append(" 套以上");
            out.append("，源码、部署文档、使用教程一起给客户。");
        } else if (sets != null) {
            out.append("\n\n课余接定制开发，同类系统做过 ").append(sets).append(" 套以上，源码和文档一起交付。");
        }

        /* 第三段：一个具体证据 */
        out.append("\n\n").append(ev).append("。");

        /* 第四段：AI 工具口径，只有 AI/Agent 岗且简历里真的写了才提 */
        boolean aiType = "ai".equals(typeKey) || "agent".equals(typeKey);
        String tools = findTools(cv);
        if (aiType && tools != null) {
            out.append("\n\n平时主要靠 ").append(tools)
               .append(" 这类工具推进，我负责拆需求、定架构和验收，代码逐段验证后才用。");
        } else if (aiType) {
            out.append("\n\n习惯是先定架构再动手，产出逐段验证，不靠「看起来能跑」下结论。");
        }

        /* 第五段：给动作 */
        out.append("\n\n").append(CLOSERS[Math.abs(seed) % CLOSERS.length]);

        return out.toString();
    }

    private static String grab(String text, String regex) {
        if (text == null || text.isEmpty()) return null;
        try {
            Matcher m = Pattern.compile(regex).matcher(text);
            if (m.find()) return m.group(1);
        } catch (Exception ignored) { }
        return null;
    }

    private static String findTools(String cv) {
        StringBuilder sb = new StringBuilder();
        String[][] map = {
                {"codex", "Codex"}, {"workbuddy", "WorkBuddy"}, {"work buddy", "WorkBuddy"},
                {"cursor", "Cursor"}, {"copilot", "Copilot"}, {"claude code", "Claude Code"}
        };
        String low = cv.toLowerCase();
        for (String[] kv : map) {
            if (low.contains(kv[0]) && sb.indexOf(kv[1]) < 0) {
                if (sb.length() > 0) sb.append("、");
                sb.append(kv[1]);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
