package com.iobTool;

import com.alibaba.fastjson.JSON;
import com.iobTool.util.FileNameUtil;
import com.iobTool.util.PublicTool;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This tool is used to quickly clear tags in unknown file
 * which will have an impact on other dic
 *
 * @Author: DanielDz
 * @Date: 2019/1/23
 */

public class ClearUnknown {
    private String unknownFilePath;
    private String knownFilePath;
    private String testFilePath;
    private String dicFilePath;
    private List<String> tagString;
    private List<String> unknownKeepList;
    private List<String> knownKeepList;

    public static void main(String[] args) throws IOException {
        System.out.println("准备开始...");
        ClearUnknown cu = new ClearUnknown();
        cu.clearKnown();
        cu.addWord();
    }

    public ClearUnknown() {
        knownKeepList = new ArrayList<>();
        tagString = new ArrayList<>();
        unknownKeepList = new ArrayList<>();
        unknownFilePath = FileNameUtil.ROOT_PATH + FileNameUtil.UNKNOWN;
        knownFilePath = FileNameUtil.ROOT_PATH + FileNameUtil.KNOWN;
        dicFilePath = FileNameUtil.ROOT_PATH + FileNameUtil.DIC;
        testFilePath = FileNameUtil.ROOT_PATH + FileNameUtil.TEST;
    }

    private void compareWord() throws IOException {
        System.out.println("去重前list的大小" + tagString.size());
        var newArray = tagString.stream().distinct().toArray();
        System.out.println("去重后list的大小" + newArray.length);
        FileReader fileReader = new FileReader(unknownFilePath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        var i = 1;
        System.out.println("准备完毕，开始处理Unknown文件...");
        while ((line = bufferedReader.readLine()) != null) {
            var isContain = false;
            for (int j = 0; j < newArray.length; j++) {
                if (line.contains(newArray[j].toString())) {
                    isContain = true;
                    break;
                }
            }
            if (!isContain)
                unknownKeepList.add(line);

            i++;
            if (i % 50 == 0)
                System.out.print(".");
            if (i % 500 == 0) {
                System.out.println();
                System.out.println("正在处理Unknown文件，已完成：" + i * 100 / 278315 + "%");
            }
        }
        System.out.println("保存中...");
        PublicTool.writeLines(testFilePath, unknownKeepList);
    }

    public void addWord() throws IOException {
        FileReader fileReader = new FileReader(dicFilePath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            var json = JSON.parseObject(line);
            var text = json.getString("text");
            var label = json.getJSONArray("label");
            var startIndex = 0;
            var endIndex = 0;
            while (startIndex < label.size()) {
                //只有一个字
                if (label.size() == 1) {
//                    if (!tagString.contains(text.substring(0, 1))) {
//                        System.out.println(text.substring(0, 1));
                    tagString.add(text.substring(0, 1));
                    break;
                }

                //找到词语在一句话中开始的位置
                for (var i = startIndex; i < label.size(); i++) {
                    if (label.get(i).toString().contains("B-")) {
                        startIndex = i;
                        break;
                    }
                    if (label.size() - 1 == i)
                        startIndex = i;
                }
                //找到词语在一句话中结束的位置
                for (var j = startIndex + 1; j < label.size(); j++) {
                    if (!label.get(j).toString().contains("I-")) {
                        endIndex = j;
                        break;
                    }
                    if (j >= label.size() - 1) {
                        endIndex = label.size();
                        break;
                    }
                }

                //如果最后一位为单独的一个字
                if (startIndex == label.size() - 1) {
                    if (label.get(label.size() - 1).toString().contains("B-")) {
//                        if (!tagString.contains(text.substring(label.size() - 1, label.size()))) {
//                            System.out.println(text.substring(label.size() - 1, label.size()));
                        tagString.add(text.substring(label.size() - 1, label.size()));

                    }
                    break;
                }

                //新词加入list
//                if (!tagString.contains(text.substring(startIndex, endIndex))) {
//                    System.out.println(text.substring(startIndex, endIndex));
                tagString.add(text.substring(startIndex, endIndex));

                startIndex = endIndex + 1;
            }
        }
        compareWord();
    }

    /**
     * In known.txt, if a line's labels are all 'O', then delete this line.
     *
     * @throws IOException
     */
    public void clearKnown() throws IOException {
        FileReader fileReader = new FileReader(knownFilePath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            var delete = true;
            var json = JSON.parseObject(line);
            var label = json.getJSONArray("label");
            for (Object tag : label)
                if (!tag.toString().equals("O"))
                    delete = false;
            if (!delete)
                knownKeepList.add(line);
        }
        PublicTool.writeLines(testFilePath, knownKeepList);
    }
}
