package com.iobTool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iobTool.util.FileNameUtil;
import com.iobTool.util.InfoUtil;
import com.iobTool.util.PublicTool;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This tool is used to change tags in iob file
 *
 * @Author: DanielDz
 * @Date: 2019/1/15
 */

public class ModifyDicTool {
    private String filePath;
    private List<String> keepLines;
    private List<String> changedLines;
    private List<String> modifyLines;

    public static void main(String[] args) {
        ModifyDicTool tool = new ModifyDicTool();
        tool.start();
    }

    public void init() {
        keepLines = new ArrayList<String>();
        modifyLines = new ArrayList<String>();
        changedLines = new ArrayList<String>();
    }

    public void selectFile() {
        System.out.println(InfoUtil.FILE_MENU);
        Scanner sc = new Scanner(System.in);
        char choice = sc.nextLine().charAt(0);
        switch (choice){
            case '1':
                filePath = FileNameUtil.ROOT_PATH + FileNameUtil.TEST;
                break;
            case '2':
                filePath = FileNameUtil.ROOT_PATH + FileNameUtil.KNOWN;
                break;
            case '3':
                filePath = FileNameUtil.ROOT_PATH + FileNameUtil.UNKNOWN;
                break;
            case '4':
                filePath = FileNameUtil.ROOT_PATH + FileNameUtil.DIC;
                break;
            default:
                System.out.println(InfoUtil.ERROR_MESSAGE);
                System.exit(1);
        }
    }

    public void start() {
        init();
        selectFile();
        System.out.println(InfoUtil.CHAR_INFO);
        Scanner sc = new Scanner(System.in);
        String word = sc.nextLine();
        try {
            readLines(filePath, word);
            modifyIob(word);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Task completed, " + changedLines.size() + " lines have changed");
    }

    public void mergeAndStore() throws IOException {
        keepLines.addAll(changedLines);
        PublicTool.writeLines(filePath, keepLines);
    }

    public void readLines(String filename, String word) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        while ((line = bufferedReader.readLine()) != null)
        {
            if(line.contains(word))
                modifyLines.add(line);
            else
                keepLines.add(line);
        }
        bufferedReader.close();
    }


    public void modifyIob(String word) throws IOException {
        String newTag = "";
        System.out.println(InfoUtil.TAG_ATTRIBUTES);
        Scanner sc = new Scanner(System.in);
        char newTagChoice = sc.nextLine().charAt(0);
        switch(newTagChoice) {
            case '1':
                newTag = InfoUtil.VIDEO_CATEGORY;
                break;
            case '2':
                newTag = InfoUtil.VIDEO_NAME;
                break;
            case '3':
                newTag = InfoUtil.LIVE_NAME;
                break;
            case '4':
                newTag = InfoUtil.APP_NAME;
                break;
            case '5':
                newTag = InfoUtil.CONTROL_TV;
                break;
            case '6':
                newTag = InfoUtil.MUSIC_NAME;
                break;
            case '7':
                newTag = InfoUtil.MUSIC_SINGER;
                break;
            case '8':
                newTag = InfoUtil.VIDEO_ACTOR;
                break;
            case '9':
                System.out.println("Please enter an attribute yourself:");
                newTag = sc.nextLine();
                break;
            default:
                System.out.println(InfoUtil.ERROR_MESSAGE);
                System.exit(1);
        }
        System.out.println("Processing...");
        for (String line : modifyLines) {
            JSONObject json = JSON.parseObject(line);
            int startIndex = json.getString("text").indexOf(word);
            int endIndex = startIndex + word.length() - 1;
            JSONArray tempLine = json.getJSONArray("label");
            for (int i = startIndex; i <= endIndex; i++) {
                if (i == startIndex)
                    tempLine.set(i, "B-" + newTag);
                else
                    tempLine.set(i, "I-" + newTag);
            }
            json.put("label", tempLine);
            System.out.println(json.toString());
            changedLines.add(json.toString());
        }
        mergeAndStore();
    }
}
