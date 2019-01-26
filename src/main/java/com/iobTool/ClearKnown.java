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
    private String testFilePath;
    private String tempFilePath;
    private List<String> knownKeepList;
    private List<String> noDuplicatedList;
    private List<String> needModifyList;

    public static void main(String[] args) throws IOException {
        ClearKnown ck = new ClearKnown();
        ck.remove();
        ck.findDuplicated();
    }

    public ClearKnown() {
        testFilePath = FileNameUtil.ROOT_PATH + FileNameUtil.TEST;
        knownFilePath = FileNameUtil.ROOT_PATH + FileNameUtil.KNOWN;
        tempFilePath = FileNameUtil.ROOT_PATH + FileNameUtil.TEMP;
        knownKeepList = new ArrayList<>();
        needModifyList = new ArrayList<>();
        noDuplicatedList = new ArrayList<>();
    }

    /**
     * find lines that contain duplicated tag word
     *
     * @throws IOException
     */
    private void findDuplicated() throws IOException {
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
                System.out.println(text);
                String newLine = text.replace(word, "");
                int count = (text.length() - newLine.length()) / word.length();
                if (count > 1) {
                    needModifyList.add(line + ";" + word);
//                    needModifyList.add(line);
                    break;
                }
                startIndex = endIndex + 1;
            }
        }
        PublicTool.writeLines(tempFilePath, needModifyList);
        dealDuplicated();
    }

    /**
     * {"text": "芭比芭比芭比芭比芭比", "label": ["B-video_name", "I-video_name", "O", "O", "O", "O", "O", "O", "O"]}
     * will only keep key word {"text": "芭比", "label": ["B-video_name", "I-video_name"]}
     *
     * @throws IOException
     */
    public void dealDuplicated() throws IOException {
        FileReader fileReader = new FileReader(tempFilePath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            var word = line.split(";")[1];
            var json = JSON.parseObject(line.split(";")[0]);
            var text = json.getString("text");
            var label = json.getJSONArray("label");
            int i = text.indexOf(word);
            int j = text.indexOf(word, i);
            var index1 = new ArrayList<Integer>();
            var index2 = new ArrayList<Integer>();
            var tempTextList = new ArrayList<String>();
            var tempLabelList = new ArrayList<String>();

            //find out the breakpoint
            while (j != -1) {
                i = text.indexOf(word, i) + word.length();
                j = text.indexOf(word, i);
                if (j != -1) {
                    index1.add(j);
                    index2.add(j + word.length());
                }
            }

            //add first chars and middle chars
            for (int k = 0; k < index1.size(); k++) {
                if (k == 0) {
                    tempTextList.add(text.substring(0, index1.get(k)));
                    tempLabelList.add(label.subList(0, index1.get(k)).toString());
                } else {
                    tempTextList.add(text.substring(index2.get(k - 1), index1.get(k)));
                    tempLabelList.add(label.subList(index2.get(k - 1), index1.get(k)).toString());
                }
            }
            //add the last chars
            tempTextList.add(text.substring(index2.get(index2.size() - 1)));
            tempLabelList.add(label.subList(index2.get(index2.size() - 1), label.size()).toString());
            //construct new text
            StringBuilder newText = new StringBuilder();
            StringBuilder newLabel = new StringBuilder();
            for (String letter : tempTextList)
                if (!letter.trim().equals(""))
                    newText.append(letter);

            for (String tag : tempLabelList)
                if (!tag.trim().equals(""))
                    newLabel.append(tag);

            //reformat labels
            newLabel = new StringBuilder(newLabel.toString().replace("][", ",").replace("[", "").replace("]", ""));
            String[] temp = newLabel.toString().split(",");
            newLabel = null;
            //the last label should not follow with a comma
            for (int t = 0; t < temp.length; t++) {
                if (t != temp.length - 1)
                    newLabel.append("\"").append(temp[t].trim()).append("\",");
                else
                    newLabel.append("\"").append(temp[t].trim()).append("\"");
            }
            System.out.println("{\"text\":\"" + newText + "\",\"label\":[" + newLabel + "]}");
            noDuplicatedList.add("{\"text\":\"" + newText + "\",\"label\":[" + newLabel + "]}");
        }
        PublicTool.writeLines(testFilePath, noDuplicatedList);
    }

    /**
     * In known.txt, if a line's labels are all “O”, then delete this line.
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
