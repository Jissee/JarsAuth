package me.jissee.jarsauth.manip;

import me.jissee.jarsauth.JarsAuth;
import me.jissee.jarsauth.gui.Locales;
import me.jissee.jarsauth.manip.jar.JarExecutor;
import me.jissee.jarsauth.manip.jar.tasks.*;
import me.jissee.jarsauth.manip.mixin.MixinConfigBuilder;
import me.jissee.jarsauth.manip.mixin.MixinRefmapBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class Pipeline {
    private static final int COPY_NUM = 1000;
    private static final int STATIC_COUNT = 5;

    private static MixinConfigBuilder configBuilder;
    private static MixinRefmapBuilder refmapBuilder;
    //1. 复制出一个dummy类，放进临时包
    //2. 将dummy类全部改成false，复制999份
    //3. 原类和dummy类全部修改数字
    //4. 将dummy移动回mixin包
    //5. boolObf
    public static Tuple<String, File> run() throws IOException{
        File inputFile = JarsAuth.getJarFile();
        if (inputFile.isDirectory()) {
            //in dev
            inputFile = new File("/Users/sun/Desktop/Minecraft/develop/jarsauth/6.0/forge/forge-1.20.1-47.4.2-mdkbkup/build/libs/jarsauth-6.0-all.jar");
        }
        String modFileName = inputFile.getName();

// 去掉 .jar 扩展名
        String baseName = modFileName.endsWith(".jar")
                ? modFileName.substring(0, modFileName.length() - 4)
                : modFileName;

        int seq = 0;

// 强制把 baseName 里的 "-signed" 和 "-signed-expanded" 去掉，避免重复
        if (baseName.contains("-signed-expanded")) {
            baseName = baseName.substring(0, baseName.indexOf("-signed-expanded"));
        } else if (baseName.contains("-signed")) {
            baseName = baseName.substring(0, baseName.indexOf("-signed"));
        }

// 检查是否有编号
        if (modFileName.matches(".*-signed-\\d+\\.jar$")) {
            // 提取序号并 +1
            String numberStr = modFileName.replaceAll(".*-signed-(\\d+)\\.jar$", "$1");
            seq = Integer.parseInt(numberStr) + 1;
        } else {
            // 初始序号 0
            seq = 0;
        }

// 构造新文件名（保证只有一个 -signed）
        String signedFileName = baseName + "-signed-" + seq + ".jar";
        String signedExpandedFileName = baseName + "-signed-expanded-" + seq + ".jar";
        String expansionMapFileName = "expansion-map-" + seq + ".txt";

        File signedFile = new File("./" + signedFileName);
        File signedExpandedFile = new File("./" + signedExpandedFileName);
        File expansionMapFile = new File("./" + expansionMapFileName);







        try(JarExecutor verificationExecutor = new JarExecutor(inputFile)){
            verificationExecutor.defineTask(new JarClassVarCheckTask<>(Integer.class, "me/jissee/jarsauth/verification/Verification", "expansionFlag", (found, itg)->{
                if (!found) {
                    throw new NotSignableException("");
                }
                int v = itg;
                if(v == 0){
                    throw new NotSignableException("");
                }
            }));
            verificationExecutor.execute();
        }catch (NotSignableException e){
            return new Tuple<>(Locales.getString("info.not.signable"), null);
        }

        try(JarExecutor configReadExecutor = new JarExecutor(inputFile)) {
            configReadExecutor.defineTask(new JarFileReadTask("jarsauth.mixins.json", (str, bin)->{
                configBuilder = new MixinConfigBuilder(str);
            }));
            configReadExecutor.defineTask(new JarFileReadTask("jarsauth.refmap.json", (str, bin)->{
                refmapBuilder = new MixinRefmapBuilder(str);
            }));
            configReadExecutor.execute();
        }

        StringBuilder exportInfo = new StringBuilder();

        List<String> originalMixinClassNames = configBuilder.getMixinClasses();

        int[] replaceTarget = new int[]{
                -114,
                -514,
                -0114,
                -0514,
                -114514,
                -1919,
                -810,
                -1919810
        };
        Set<Integer> replaceTargetValues = new HashSet<>(
                Arrays.stream(replaceTarget)
                .boxed()
                .toList()
        );
        Map<Integer, Integer> replaceMapping = new HashMap<>();


        Random replaceRandom = new Random();
        int replaceRangeBase = replaceRandom.nextInt(Integer.MIN_VALUE + 1, COPY_NUM * -20);
        List<Integer> replaceCandidateRange = Range.getLinkedRange(replaceRangeBase, replaceRangeBase + COPY_NUM * 10);
        Collections.shuffle(replaceCandidateRange);

        for(Integer value : replaceTargetValues){
            replaceMapping.put(value, replaceCandidateRange.get(0));
            replaceCandidateRange.remove(0);
        }

        try(JarExecutor replaceNumExecutor = new JarExecutor(inputFile)){
            JarClassNumModificationTask task = new JarClassNumModificationTask();
            task.addTarget("me/jissee/jarsauth/mixin/*")
                .addTarget("me/jissee/jarsauth/pending/*");

            for(Integer oldValue : replaceTarget) {
                int newValue = replaceMapping.get(oldValue);
                task.addReplace(oldValue, newValue);
            }
            replaceNumExecutor.defineTask(task);
            replaceNumExecutor.defineTask(new JarClassNumModificationTask().addTarget("me/jissee/jarsauth/verification/Verification").addReplace(2, 0));
            replaceNumExecutor.execute();
            replaceNumExecutor.export(signedFile);
            exportInfo.append(String.format(Locales.getString("info.exported.signed"), signedFile.getAbsolutePath())).append("\n");
        }


        try(JarExecutor expansionExecutor = new JarExecutor(inputFile)){
            for(String mixinClass : originalMixinClassNames) {
                expansionExecutor.defineTask(new JarClassDuplicationTask("me/jissee/jarsauth/mixin/" + mixinClass, "me/jissee/jarsauth/dummy/" + mixinClass + "Dummy", List.of(1)));
            }
            expansionExecutor.defineTask(new JarClassNumModificationTask().addTarget("me/jissee/jarsauth/dummy/*").addReplace(1, 0));

            RandomStringGenerator generator = new RandomStringGenerator(COPY_NUM * 6, 20);
            List<String> nameCandidate = generator.toList();




            List<Integer> allIndices = new ArrayList<>();

            int baseIndex = 0;
            Map<Integer,String> reverseMap = new HashMap<>();

            for(String mixinClass : originalMixinClassNames) {
                List<Integer> indicesForCurrentClass = Range.getLinkedRange(baseIndex + 1, baseIndex + COPY_NUM);
                allIndices.add(baseIndex);
                allIndices.addAll(indicesForCurrentClass);
                String originalName = "me/jissee/jarsauth/mixin/" + mixinClass;
                reverseMap.put(baseIndex, originalName);
                indicesForCurrentClass.forEach(index -> reverseMap.put(index, originalName));

                expansionExecutor
                        .defineTask(new JarClassMotionTask().add(originalName, "me/jissee/jarsauth/dummy/M" + baseIndex))
                        .defineTask(new JarClassDuplicationTask("me/jissee/jarsauth/dummy/" + mixinClass + "Dummy", "me/jissee/jarsauth/dummy/M#", indicesForCurrentClass));


                baseIndex = baseIndex + COPY_NUM;
            }

            Collections.shuffle(allIndices);


            JarClassNumModificationTask numModTask = new JarClassNumModificationTask();
            JarClassMotionTask motionTask = new JarClassMotionTask();
            StringBuilder expMap = new StringBuilder();
            for(int i = 0; i < allIndices.size(); i++){
                String originalName = reverseMap.get(i);
                String midName = "me/jissee/jarsauth/dummy/M" + i;
                String finalName = "me/jissee/jarsauth/mixin/M" + allIndices.get(i);

                if(i % COPY_NUM != 0){
                    for(int fieldIndex = 1; fieldIndex <= STATIC_COUNT; fieldIndex++){
                        String fieldName = "replaceTarget" + fieldIndex;
                        String originalClassFieldName = originalName + "#" + fieldName;
                        if(replaceTargetValues.containsKey(originalClassFieldName)){
                            int target = replaceTargetValues.get(originalClassFieldName);
                            int destValue = replaceCandidateRange.get(0);
                            numModTask.add(midName, target, destValue);
                            replaceCandidateRange.remove(0);
                        }
                    }
                }else{
                    for(int fieldIndex = 1; fieldIndex <= STATIC_COUNT; fieldIndex++){
                        String fieldName = "replaceTarget" + fieldIndex;
                        String originalClassFieldName = originalName + "#" + fieldName;
                        if(replaceTargetValues.containsKey(originalClassFieldName)){
                            int target = replaceTargetValues.get(originalClassFieldName);
                            int destValue = replaceMapping.get(target);
                            numModTask.add(midName, target, destValue);
                        }
                    }
                }

                motionTask.add(midName, finalName);
                int finalI = i;
                expansionExecutor.defineTask(new JarCustomTask(()->{
                    refmapBuilder.addObf(reverseMap.get(finalI), finalName);
                }));
                expMap.append(i).append(" -> ").append(allIndices.get(i)).append("\n");
            }

            expansionExecutor.defineTask(numModTask).defineTask(motionTask);
            Files.writeString(expansionMapFile.toPath(), expMap.toString());

            exportInfo.append(String.format(Locales.getString("info.exported.expansion.map"), expansionMapFile.getAbsolutePath())).append('\n');

            expansionExecutor.defineTask(new JarClassNumModificationTask().addTarget("me/jissee/jarsauth/verification/Verification").addReplace(1, 0));
            expansionExecutor.defineTask(new JarClassNumModificationTask().addTarget("me/jissee/jarsauth/verification/Verification").addReplace(2, 0));

            JarClassStaticFinalFieldRemovalTask task = new JarClassStaticFinalFieldRemovalTask();
            for(int i = 0; i < allIndices.size(); i++){
                for(int fieldIndex = 1; fieldIndex <= STATIC_COUNT; fieldIndex++){
                    task.add("me/jissee/jarsauth/mixin/M" + i, "replaceTarget" + fieldIndex);
                }
            }
            expansionExecutor.defineTask(task);

            //executor.defineTask(new JarBooleanObfuscationTask());

            configBuilder.removeAll();
            for(int i = 0; i < allIndices.size(); i++){
                configBuilder.add("M" + i);
            }
            expansionExecutor.defineTask(new JarFileAdditionTask("jarsauth.mixins.json", JarFileAdditionTask.supplierWrap(()->configBuilder.toString())));
            expansionExecutor.defineTask(new JarFileAdditionTask("jarsauth.refmap.json", JarFileAdditionTask.supplierWrap(()->refmapBuilder.toStringObf())));
            expansionExecutor.execute();
            expansionExecutor.export(signedExpandedFile);
            exportInfo.append(String.format(Locales.getString("info.exported.signed.expanded"), signedExpandedFile.getAbsolutePath())).append('\n');

            return new Tuple<>(exportInfo.toString(), signedExpandedFile);
        }
    }
}
