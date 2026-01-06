package me.jissee.jarsauth.manip;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RandomStringGenerator {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private final Set<String> uniqueStrings = new HashSet<>();

    public RandomStringGenerator(int count, int length) {
        Random random = new Random();
        while (uniqueStrings.size() < count) {
            String str = generateRandomString(length, random);
            uniqueStrings.add(str); // HashSet 保证唯一性
        }
    }

    public static String generateRandomString(int length, Random random) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public File toFile() {
        File file = new File("dict.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String s : uniqueStrings) {
                writer.write(s);
                writer.newLine();
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> toList() {
        return new ArrayList<>(uniqueStrings);
    }
}
