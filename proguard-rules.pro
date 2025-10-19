# 禁用优化和预验证，简化调试
-dontoptimize
-dontpreverify
-dontwarn **

# 混淆所有类、字段和方法，名称随机生成
# 默认行为即可，不需要额外keep

# 保留入口点
-keep class me.jissee.jarsauth.gui.JarsAuthGui {
    public static void main(java.lang.String[]);
}