package me.jissee.jarsauth.manip;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class RandomStringGeneratorToFile {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String generateRandomString(int length, Random random) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public static File generate() {
        Random random = new Random();
        int count = 10000;
        int length = 10;

        Set<String> uniqueStrings = new HashSet<>();
        while (uniqueStrings.size() < count) {
            String str = generateRandomString(length, random);
            uniqueStrings.add(str); // HashSet 保证唯一性
        }
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
}
