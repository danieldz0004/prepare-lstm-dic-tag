package com.iobTool;

import com.alibaba.fastjson.JSON;
import com.iobTool.util.FileNameUtil;
import com.iobTool.util.PublicTool;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 1. remove unknown lines in known.txt
 * 2. deal with duplicated lines
 *
 * @author: DanielDz
 * @Date: 2019/Jan/24
 */

public class ClearKnown {

    private String knownFilePath;
    private String tempFilePath;
    private List<String> knownKeepList;
    private List<String> needModifyList;

    public static void main(String[] args) throws IOException {
        ClearKnown ck = new ClearKnown();
//        ck.remove();
        ck.dealDuplicated();
    }

    public ClearKnown() {
        knownFilePath = FileNameUtil.ROOT_PATH + FileNameUtil.KNOWN;
        tempFilePath = FileNameUtil.ROOT_PATH + FileNameUtil.TEMP;
        knownKeepList = new ArrayList<>();
        needModifyList = new ArrayList<>();
    }

    /**
     * {"text": "芭比芭比芭比芭比芭比", "label": ["B-video_name", "I-video_name", "O", "O", "O", "O", "O", "O", "O"]}
     * will only keep key word {"text": "芭比", "label": ["B-video_name", "I-video_name"]}
     *
     * @throws IOException
     */
    private void dealDuplicated() throws IOException {
        FileReader fileReader = new FileReader(knownFilePath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            var startIndex = 0;
            var endIndex = 0;
            var json = JSON.parseObject(line);
            var text = json.getString("text");
            var label = json.getJSONArray("label");

            while (startIndex < label.size() && label.size() != 1) {
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

                if (startIndex == label.size() - 1)
                    break;

                String word = text.substring(startIndex, endIndex);
                String newLine = text.replace(word, "");
                int count = (text.length() - newLine.length()) / word.length();
                if (count > 1) {
                    needModifyList.add(line);
                    break;
                }

                startIndex = endIndex + 1;
            }
        }
        PublicTool.writeLines(tempFilePath, needModifyList);
    }

    /**
     * In known.txt, if a line's labels are all 'O', then delete this line.
     *
     * @throws IOException
     */
    public void remove() throws IOException {
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
        PublicTool.writeLines(knownFilePath, knownKeepList);
        knownKeepList = new ArrayList<>();
    }
}
