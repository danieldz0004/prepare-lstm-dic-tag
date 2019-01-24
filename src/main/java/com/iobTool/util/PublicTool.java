package com.iobTool.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class PublicTool {

    public static void writeLines(String filename, List<String> list) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
        for (String line : list)
            writer.write(line + "\r\n");
        writer.close();
        fos.close();
    }
}
