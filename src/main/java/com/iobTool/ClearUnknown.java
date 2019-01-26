package com.iobTool;

import com.alibaba.fastjson.JSON;
import com.iobTool.util.FileNameUtil;
import com.iobTool.util.PublicTool;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This tool is used to quickly clear tags in unknown file
 * which will have an impact on other dic
 *
 * @Author: DanielDz
 * @Date: 2019/Jan/23
 */

public class ClearUnknown {
    private String unknownFilePath;
    private String testFilePath;
    private String dicFilePath;
    private List<String> tagString;
    private List<String> unknownKeepList;

    public static void main(String[] args) throws IOException {
        System.out.println("准备开始...");
        ClearUnknown cu = new ClearUnknown();
        cu.start();
    }

    private ClearUnknown() {
        tagString = new ArrayList<>();
        unknownKeepList = new ArrayList<>();
        unknownFilePath = FileNameUtil.ROOT_PATH + FileNameUtil.UNKNOWN;
        dicFilePath = FileNameUtil.ROOT_PATH + FileNameUtil.DIC;
        testFilePath = FileNameUtil.ROOT_PATH + FileNameUtil.TEST;
    }

    /**
     * word: ABCDE => [AB, BC, CD, DE, ABC, BCD, CDE, ABCD, BCDE]
     *
     * @param list
     */
    private ArrayList<Object> enumerate(Object[] list) {
        var tempList = new ArrayList<>();
        for (Object object : list) {
            String word = object.toString();
            if (word.length() > 3)
                for (int i = 3; i < word.length(); i++)
                    for (int j = 0; j <= word.length() - i; j++)
                        tempList.add(word.substring(j, j + i));
        }
        return tempList;
    }

    private void compareWord() throws IOException {
        System.out.println("去重前list的大小" + tagString.size());
        var newArray = tagString.stream().distinct().toArray();
        System.out.println("去重后list的大小" + newArray.length);
        var newList = enumerate(newArray).stream().distinct().collect(Collectors.toList());
        FileReader fileReader = new FileReader(unknownFilePath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        var i = 1;
        System.out.println("准备完毕，开始处理Unknown文件...");
        while ((line = bufferedReader.readLine()) != null) {
            var isContain = false;
            for (int j = 0; j < newList.size(); j++) {
                if (line.contains(newList.get(j).toString())) {
                    isContain = true;
                    break;
                }
            }
            if (!isContain)
                unknownKeepList.add(line);

            i++;
            if (i % 50 == 0)
                System.out.print(".");
            if (i % 2500 == 0) {
                System.out.println();
                System.out.println("正在处理Unknown文件，已完成：" + i * 100 / 278315 + "%");
            }
        }
        System.out.println("保存中...");
        PublicTool.writeLines(testFilePath, unknownKeepList);
    }

    private void start() throws IOException {
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
                    if (label.get(label.size() - 1).toString().contains("B-"))
                        tagString.add(text.substring(label.size() - 1, label.size()));
                    break;
                }

                //新词加入list
                tagString.add(text.substring(startIndex, endIndex));

                startIndex = endIndex + 1;
            }
        }
        compareWord();
    }
}
