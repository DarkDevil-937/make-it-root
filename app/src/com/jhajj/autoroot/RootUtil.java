package com.jhajj.autoroot;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public final class RootUtil {
    private static volatile String client;

    private RootUtil() {}

    public static void init(Context ctx) {
        client = ctx.getApplicationInfo().nativeLibraryDir + "/libglroot.so";
    }

    public static String client() {
        return client;
    }

    public static String checkRoot() {
        String c = client;
        if (c == null || !new File(c).exists()) {
            return "Not rooted (client missing)";
        }
        try {
            Process p = new ProcessBuilder(c, "--su", "-c", "id")
                    .redirectErrorStream(true)
                    .start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            int rc = p.waitFor();
            String out = sb.toString().trim();
            if (rc == 0 && out.contains("uid=0")) {
                return "ROOTED\n" + out;
            }
            return "Not rooted\n" + out;
        } catch (Exception e) {
            return "Not rooted (" + e.getMessage() + ")";
        }
    }
}
