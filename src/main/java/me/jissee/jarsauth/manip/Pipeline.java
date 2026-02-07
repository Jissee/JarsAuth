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
    private static final int COPY_NUM = 79;
    private static final int STATIC_COUNT = 5;

    private static MixinConfigBuilder configBuilder;
    private static MixinRefmapBuilder refmapBuilder;

    public static Tuple<String, File> run() throws IOException{
        File inputFile = JarsAuth.getJarFile();
        String modFileName = inputFile.getName();

// 去掉 .jar 扩展名
        String baseName = modFileName.endsWith(".jar")
                ? modFileName.substring(0, modFileName.length() - 4)
                : modFileName;



// 强制把 baseName 里的 "-signed" 和 "-signed-expanded" 去掉，避免重复
        if (baseName.contains("-signed-expanded")) {
            baseName = baseName.substring(0, baseName.indexOf("-signed-expanded"));
        } else if (baseName.contains("-signed")) {
            baseName = baseName.substring(0, baseName.indexOf("-signed"));
        }

// 构造新文件名（保证只有一个 -signed）
        String signedFileName = baseName + "-signed" + ".jar";
        String signedExpandedFileName = baseName + "-signed-expanded" + ".jar";
        String expansionMapFileName = "expansion-map" + ".txt";

        File signedFile = new File("./" + signedFileName);
        File signedExpandedFile = new File("./" + signedExpandedFileName);
        File expansionMapFile = new File("./" + expansionMapFileName);

        StringBuilder sb = new StringBuilder();
        if(signedFile.exists()){
            sb.append('\n').append(signedFile.getAbsolutePath());
        }
        if(signedExpandedFile.exists()){
            sb.append('\n').append(signedExpandedFile.getAbsolutePath());
        }
        if(expansionMapFile.exists()){
            sb.append('\n').append(expansionMapFile.getAbsolutePath());
        }

        if(!sb.isEmpty()){
            throw new IOException(String.format(Locales.getString("info.file.exists"), sb.toString()));
        }






        try(JarExecutor verificationExecutor = new JarExecutor(inputFile)){
            verificationExecutor.defineTask(new JarClassVarCheckTask<>(Integer.class, "me/jissee/jarsauth/verification/Verification", "signedFlag", (found, itg)->{
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

        List<String> originalCommonMixinClassNames = configBuilder.getCommonMixinClasses();
        List<String> originalClientMixinClassNames = configBuilder.getClientMixinClasses();

        int[] replaceTarget = new int[]{
                -114,
                -514,
                -0114,
                -0514,
                -114514,
                -1919,
                -810,
                -191,
                -9810
        };
        Set<Integer> replaceTargetValues = new HashSet<>(
                Arrays.stream(replaceTarget)
                        .boxed()
                        .toList()
        );
        Map<Integer, Integer> replaceMapping = new HashMap<>();


        Random replaceRandom = new Random();
        int replaceRangeBase = replaceRandom.nextInt(Integer.MIN_VALUE + 1, Integer.MIN_VALUE / 2);
        List<Integer> replaceCandidates = Range.getLinkedRange(replaceRangeBase, replaceRangeBase + COPY_NUM * 1000);
        Collections.shuffle(replaceCandidates);

        for(Integer value : replaceTargetValues){
            replaceMapping.put(value, replaceCandidates.get(0));
            replaceCandidates.remove(0);
        }

        try(JarExecutor replaceNumExecutor = new JarExecutor(inputFile)){
            JarClassNumModificationTask task = new JarClassNumModificationTask();
            task.addTarget("me/jissee/jarsauth/mixin/*")
                    .addTarget("me/jissee/jarsauth/pending/*")
                    .addTarget("me/jissee/jarsauth/event/*");

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


        try(JarExecutor expansionExecutor = new JarExecutor(signedFile)){
            for(String mixinClass : originalCommonMixinClassNames) {
                expansionExecutor.defineTask(new JarClassDuplicationTask("me/jissee/jarsauth/mixin/" + mixinClass, "me/jissee/jarsauth/dummy/" + mixinClass + "Dummy", List.of(1)));
            }
            for(String mixinClass : originalClientMixinClassNames) {
                expansionExecutor.defineTask(new JarClassDuplicationTask("me/jissee/jarsauth/mixin/" + mixinClass, "me/jissee/jarsauth/dummy/" + mixinClass + "Dummy", List.of(1)));
            }
            expansionExecutor.defineTask(new JarClassNumModificationTask().addTarget("me/jissee/jarsauth/dummy/*").addReplace(1, 0));

            RandomStringGenerator generator = new RandomStringGenerator(COPY_NUM * 20, 20);
            List<String> nameCandidate = generator.toList();
            StringBuilder expMap = new StringBuilder();
            List<String> realNamesCommon = new ArrayList<>();
            List<String> realNamesClient = new ArrayList<>();

            for(String mixinClass : originalCommonMixinClassNames) {
                String obfName = nameCandidate.get(0);
                nameCandidate.remove(0);
                realNamesCommon.add(obfName);
                expMap.append(mixinClass).append(" -> ").append(obfName).append("\n");

                JarClassMotionTask task = new JarClassMotionTask().add("me/jissee/jarsauth/mixin/" + mixinClass, "me/jissee/jarsauth/mixin/" + obfName);
                if(refmapBuilder != null) {
                    refmapBuilder.addObf("me/jissee/jarsauth/mixin/" + mixinClass, "me/jissee/jarsauth/mixin/" + obfName);
                }
                expansionExecutor.defineTask(task);
            }
            for(String mixinClass : originalClientMixinClassNames) {
                String obfName = nameCandidate.get(0);
                nameCandidate.remove(0);
                realNamesClient.add(obfName);
                expMap.append(mixinClass).append(" -> ").append(obfName).append("\n");

                JarClassMotionTask task = new JarClassMotionTask().add("me/jissee/jarsauth/mixin/" + mixinClass, "me/jissee/jarsauth/mixin/" + obfName);
                if(refmapBuilder != null) {
                    refmapBuilder.addObf("me/jissee/jarsauth/mixin/" + mixinClass, "me/jissee/jarsauth/mixin/" + obfName);
                }
                expansionExecutor.defineTask(task);
            }


            int commonIndex = 0;
            Map<Integer, String> reverseMap = new HashMap<>();
            for(String mixinClass : originalCommonMixinClassNames) {
                List<Integer> indicesForCurrentClass = Range.getLinkedRange(commonIndex, commonIndex + COPY_NUM);
                indicesForCurrentClass.forEach(index -> {
                    reverseMap.put(index, mixinClass);
                });
                expansionExecutor
                        .defineTask(new JarClassDuplicationTask(
                                "me/jissee/jarsauth/dummy/" + mixinClass + "Dummy",
                                "me/jissee/jarsauth/dummy/M#",
                                indicesForCurrentClass
                        ));
                commonIndex += COPY_NUM;
            }
            int clientIndex = commonIndex;
            for(String mixinClass : originalClientMixinClassNames) {
                List<Integer> indicesForCurrentClass = Range.getLinkedRange(clientIndex, clientIndex + COPY_NUM);
                indicesForCurrentClass.forEach(index -> {
                    reverseMap.put(index, mixinClass);
                });
                expansionExecutor
                        .defineTask(new JarClassDuplicationTask(
                                "me/jissee/jarsauth/dummy/" + mixinClass + "Dummy",
                                "me/jissee/jarsauth/dummy/M#",
                                indicesForCurrentClass
                        ));
                clientIndex += COPY_NUM;
            }

            for(int i = 0; i < clientIndex; i++) { // all classes
                JarClassNumModificationTask modDummyTask = new JarClassNumModificationTask();
                modDummyTask.addTarget("me/jissee/jarsauth/dummy/M" + i);
                for(Integer originalSigned : replaceMapping.values()) {
                    int target = replaceCandidates.get(0);
                    replaceCandidates.remove(0);
                    modDummyTask.addReplace(originalSigned, target);
                }
                expansionExecutor.defineTask(modDummyTask);
            }

            Set<String> usedObfNamesCommon = new HashSet<>();
            Set<String> usedObfNamesClient = new HashSet<>();
            for(int i = 0; i < commonIndex; i++) {
                String obfName = nameCandidate.get(0);
                nameCandidate.remove(0);
                usedObfNamesCommon.add(obfName);
                expansionExecutor.defineTask(new JarClassMotionTask()
                        .add(
                                "me/jissee/jarsauth/dummy/M" + i,
                                "me/jissee/jarsauth/mixin/" + obfName
                        )
                );
                if(refmapBuilder != null) {
                    refmapBuilder.addObf("me/jissee/jarsauth/mixin/" + reverseMap.get(i), "me/jissee/jarsauth/mixin/" + obfName);
                }
            }
            for(int i = commonIndex; i < clientIndex; i++) {
                String obfName = nameCandidate.get(0);
                nameCandidate.remove(0);
                usedObfNamesClient.add(obfName);
                expansionExecutor.defineTask(new JarClassMotionTask()
                        .add(
                                "me/jissee/jarsauth/dummy/M" + i,
                                "me/jissee/jarsauth/mixin/" + obfName
                        )
                );
                if(refmapBuilder != null) {
                    refmapBuilder.addObf("me/jissee/jarsauth/mixin/" + reverseMap.get(i), "me/jissee/jarsauth/mixin/" + obfName);
                }
            }



            Collection<Integer> occupied = replaceMapping.values();
            JarClassNumModificationTask numModTask = new JarClassNumModificationTask();

            for(Integer value : occupied){
                int replaceDest = replaceCandidates.get(0);
                replaceCandidates.remove(0);
                numModTask.addReplace(value, replaceDest);
            }

            for(String obfName : usedObfNamesCommon) {
                numModTask.addTarget("me/jissee/jarsauth/mixin/" + obfName);
            }
            for(String obfName : usedObfNamesClient) {
                numModTask.addTarget("me/jissee/jarsauth/mixin/" + obfName);
            }


            expansionExecutor.defineTask(numModTask);
            Files.writeString(expansionMapFile.toPath(), expMap.toString());

            exportInfo.append(String.format(Locales.getString("info.exported.expansion.map"), expansionMapFile.getAbsolutePath())).append('\n');

            expansionExecutor.defineTask(new JarClassNumModificationTask().addTarget("me/jissee/jarsauth/verification/Verification").addReplace(2, 0));

            JarClassStaticFinalFieldRemovalTask task = new JarClassStaticFinalFieldRemovalTask();

            for(int fieldIndex = 1; fieldIndex <= STATIC_COUNT; fieldIndex++){
                for(String obfName : realNamesCommon) {
                    String internalName = "me/jissee/jarsauth/mixin/" + obfName;
                    task.add(internalName, "replaceTarget" + fieldIndex);
                }
                for(String obfName : usedObfNamesCommon) {
                    String internalName = "me/jissee/jarsauth/mixin/" + obfName;
                    task.add(internalName, "replaceTarget" + fieldIndex);
                }
                for(String obfName : realNamesClient) {
                    String internalName = "me/jissee/jarsauth/mixin/" + obfName;
                    task.add(internalName, "replaceTarget" + fieldIndex);
                }
                for(String obfName : usedObfNamesClient) {
                    String internalName = "me/jissee/jarsauth/mixin/" + obfName;
                    task.add(internalName, "replaceTarget" + fieldIndex);
                }
            }
            expansionExecutor.defineTask(task);

            expansionExecutor.defineTask(new JarClassIntegerObfuscationTask());

            configBuilder.removeAllCommon();
            configBuilder.addCommonAll(realNamesCommon);
            configBuilder.addCommonAll(usedObfNamesCommon);
            configBuilder.removeAllClient();
            configBuilder.addClientAll(realNamesClient);
            configBuilder.addClientAll(usedObfNamesClient);
            expansionExecutor.defineTask(new JarFileAdditionTask("jarsauth.mixins.json", JarFileAdditionTask.supplierWrap(()->configBuilder.toString())));
            if(refmapBuilder != null) {
                expansionExecutor.defineTask(new JarFileAdditionTask("jarsauth.refmap.json", JarFileAdditionTask.supplierWrap(()->refmapBuilder.toStringObf())));
            }
            expansionExecutor.execute();
            expansionExecutor.export(signedExpandedFile);
            exportInfo.append(String.format(Locales.getString("info.exported.signed.expanded"), signedExpandedFile.getAbsolutePath())).append('\n');

            return new Tuple<>(exportInfo.toString(), signedExpandedFile);
        }
    }
}
