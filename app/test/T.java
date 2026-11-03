package com.liule.talkhelper;

public class T {
    public static void main(String[] a) {
        String resume =
            "某某大学 计算机科学与技术，2027 届本科，课程已修完，可随时到岗。\n"
          + "【定制交付】课余做软件定制开发，累计交付 40+ 项，其中各类管理系统 8 套以上。\n"
          + "【技术栈】Java：Spring Boot + MyBatis-Plus + JWT；前端：Vue3 + Element Plus。\n"
          + "【工作方式】熟练使用 Codex、WorkBuddy 等 AI 编程工具，负责需求拆解、架构设计与结果验收。";

        String jdJava =
            "java开发工程师\n7分钟前回复\n武汉中地数字孪生技术有限公司\n未融资 · 20-99人 · 计算机软件\n"
          + "岗位职责：1、负责公司产品后端开发，基于 Spring Boot + MyBatis 框架；"
          + "2、负责数据库表结构设计与接口开发；3、配合前端完成联调。";

        String jdAgent =
            "AI Agent 开发工程师\n20-30K\n武汉\n某某数据科技有限公司\n"
          + "岗位职责：1、负责智能体 Agent 架构设计，基于 Function Calling 实现工具调用；"
          + "2、设计任务规划与记忆机制；3、优化工具调用准确率，降低模型幻觉。";

        String[] jds = {jdJava, jdAgent};
        String[] names = {"Java 岗（含简历）", "Agent 岗（含简历）"};
        for (int i = 0; i < jds.length; i++) {
            TalkGen.Result r = TalkGen.parse(jds[i]);
            System.out.println("========== " + names[i] + " ==========");
            System.out.println("识别类型: " + r.type + " | 公司: " + r.company + " | 岗位: " + r.title);
            String out = TalkGen.build(r, r.type, 0, resume);
            System.out.println(out);
            System.out.println("[字数] " + out.replaceAll("\\s", "").length());
            System.out.println();
        }

        System.out.println("========== 无简历（通用） ==========");
        TalkGen.Result r2 = TalkGen.parse(jdJava);
        System.out.println(TalkGen.build(r2, r2.type, 0, ""));
    }
}
