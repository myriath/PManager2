package com.example.mitch.pmanager;

import static com.example.mitch.pmanager.Util.bytesToChars;
import static com.example.mitch.pmanager.Util.charsToBytes;
import static com.example.mitch.pmanager.Util.removeExtension;
import static com.example.mitch.pmanager.Util.splitByChar;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/**
 * Tests various utility methods from the Util class
 */
public class UtilTest {

    /**
     * Performs a group of various split tests with different test strings and a random number string
     */
    @Test
    public void splitByCharTest() {
        Random random = new Random();
        splitByCharSingleTest("Hello world!", "\n");
        splitByCharSingleTest("Hello\nworld!", "\n");
        splitByCharSingleTest("\nHello\nworld!\n", "\n");
        splitByCharSingleTest("\n\na\n\n", "\n");
        splitByCharSingleTest("\n\n\n\n", "\n");
        splitByCharSingleTest(String.valueOf(random.nextLong()), String.valueOf(Math.abs(random.nextInt() % 10)));
    }

    /**
     * Does a single split test given a string and regex.
     * @param s String to test
     * @param regex Regex to split by
     */
    public static void splitByCharSingleTest(String s, String regex) {
        String stringArray = Arrays.toString(s.split(regex));
        String charsArray = splitCharsToString(splitByChar(s.toCharArray(), regex.charAt(0)));
        assertEquals(stringArray, charsArray);
    }

    /**
     * Mimics Arrays.toString(String.split())
     * @param test char[][] version of String.split() to test
     * @return String formatted like Arrays.toString(String.split())
     */
    public static String splitCharsToString(char[][] test) {
        String[] strings = new String[test.length];
        for (int i = 0; i < test.length; i++) {
            strings[i] = String.valueOf(test[i]);
        }
        return Arrays.toString(strings);
    }

    /**
     * Performs conversion test with a test "Hello world" string, and a random number string
     */
    @Test
    public void byteCharConversionTest() {
        Random random = new Random();
        conversionSingleTest("Hello world!ᴙ");
        conversionSingleTest(String.valueOf(random.nextLong()));

    }

    /**
     * Performs a single conversion test with a given test string
     *
     * @param testString String to test
     */
    public void conversionSingleTest(String testString) {
        char[] charsToBytesTest = testString.toCharArray();
        byte[] bytesToCharsTest = testString.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(bytesToCharsTest, charsToBytes(charsToBytesTest));
        assertArrayEquals(charsToBytesTest, bytesToChars(bytesToCharsTest));
    }

    @Test
    public void testExtensionRemoval() {
        assertEquals("filename", removeExtension("filename.str"));
        assertEquals("abc.def", removeExtension("abc.def.g"));
        assertEquals("bill.cosby", removeExtension("bill.cosby.orgx"));
    }
}