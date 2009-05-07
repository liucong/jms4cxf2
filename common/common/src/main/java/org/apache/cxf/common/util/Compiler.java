/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.common.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.helpers.FileUtils;

public class Compiler {
    public boolean compileFiles(String[] files, File outputDir) {
        List<String> list = new ArrayList<String>();

        // Start of honoring java.home for used javac
        String fsep = System.getProperty("file.separator");
        String javacstr = "javac";
        String platformjavacname = "javac";

        if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
            platformjavacname = "javac.exe";
        }

        if (new File(System.getProperty("java.home") + fsep + platformjavacname).exists()) {
            // check if java.home is jdk home
            javacstr = System.getProperty("java.home") + fsep + platformjavacname;
        } else if (new File(System.getProperty("java.home") + fsep + ".." + fsep + "bin" + fsep
                            + platformjavacname).exists()) {
            // check if java.home is jre home
            javacstr = System.getProperty("java.home") + fsep + ".." + fsep + "bin" + fsep
                       + platformjavacname;
        }

        list.add(javacstr);
        // End of honoring java.home for used javac

        // This code doesn't honor java.home
        // list.add("javac");

        //fix for CXF-2081, set maximum heap of this VM to javac.
        list.add("-J-Xmx" + Runtime.getRuntime().maxMemory());

        if (outputDir != null) {
            list.add("-d");
            list.add(outputDir.getAbsolutePath().replace(File.pathSeparatorChar, '/'));
        }

        String javaClasspath = System.getProperty("java.class.path");
        boolean classpathSetted = javaClasspath != null ? true : false;
        if (!classpathSetted) {
            list.add("-extdirs");
            list.add(getClass().getClassLoader().getResource(".").getFile() + "../lib/");
        } else {
            list.add("-classpath");
            list.add(javaClasspath);
        }

        int idx = list.size();
        list.addAll(Arrays.asList(files));

        return internalCompile(list.toArray(new String[list.size()]), idx);
    }

    public boolean internalCompile(String[] args, int sourceFileIndex) {
        Process p = null;
        String cmdArray[] = null;
        File tmpFile = null;
        try {
            if (isLongCommandLines(args) && sourceFileIndex >= 0) {
                PrintWriter out = null;
                tmpFile = FileUtils.createTempFile("cxf-compiler", null);
                out = new PrintWriter(new FileWriter(tmpFile));
                for (int i = sourceFileIndex; i < args.length; i++) {
                    if (args[i].indexOf(" ") > -1) {
                        args[i] = args[i].replace(File.separatorChar, '/');
                        //
                        // javac gives an error if you use forward slashes
                        // with package-info.java. Refer to:
                        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6198196
                        //
                        if (args[i].indexOf("package-info.java") > -1
                            && System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
                            out.println("\"" + args[i].replaceAll("/", "\\\\\\\\") + "\"");
                        } else {
                            out.println("\"" + args[i] + "\"");
                        }
                    } else {
                        out.println(args[i]);
                    }
                }
                out.flush();
                out.close();
                cmdArray = new String[sourceFileIndex + 1];
                System.arraycopy(args, 0, cmdArray, 0, sourceFileIndex);
                cmdArray[sourceFileIndex] = "@" + tmpFile;
            } else {
                cmdArray = new String[args.length];
                System.arraycopy(args, 0, cmdArray, 0, args.length);
            }

            if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
                for (int i = 0; i < cmdArray.length; i++) {
                    if (cmdArray[i].indexOf("package-info") == -1) {
                        cmdArray[i] = cmdArray[i].replace('\\', '/');
                    }
                }
            }

            p = Runtime.getRuntime().exec(cmdArray);

            if (p.getErrorStream() != null) {
                StreamPrinter errorStreamPrinter = new StreamPrinter(p.getErrorStream(), "", System.out);
                errorStreamPrinter.run();
            }

            if (p.getInputStream() != null) {
                StreamPrinter infoStreamPrinter = new StreamPrinter(p.getInputStream(), "[INFO]", System.out);
                infoStreamPrinter.run();
            }

            if (p != null) {
                return p.waitFor() == 0 ? true : false;
            }
        } catch (SecurityException e) {
            System.err.println("[ERROR] SecurityException during exec() of compiler \"" + args[0] + "\".");
        } catch (InterruptedException e) {
            // ignore

        } catch (IOException e) {
            System.err.print("[ERROR] IOException during exec() of compiler \"" + args[0] + "\"");
            System.err.println(". Check your path environment variable.");
        } finally {
            if (tmpFile != null && tmpFile.exists()) {
                FileUtils.delete(tmpFile);
            }
        }

        return false;
    }

    private boolean isLongCommandLines(String args[]) {
        StringBuffer strBuffer = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
            strBuffer.append(args[i]);
        }
        return strBuffer.toString().length() > 4096 ? true : false;
    }
}
