package com.elderdrivers.riru.edxp.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;

public class MetaDataReader {
    private final HashMap<String, Object> metaData = new HashMap<>();

    public static Map<String, Object> getMetaData(File apk) throws IOException {
        return new MetaDataReader(apk).metaData;
    }

    private MetaDataReader(File apk) throws IOException {
        JarFile zip = new JarFile(apk);
        InputStream is = zip.getInputStream(zip.getEntry("AndroidManifest.xml"));
        byte[] bytes =  getBytesFromInputStream(is);
        AxmlReader reader = new AxmlReader(bytes);
        reader.accept(new AxmlVisitor() {
            @Override
            public NodeVisitor child(String ns, String name) {
                NodeVisitor child = super.child(ns, name);
                return new ManifestTagVisitor(child);
            }
        });
    }

    public static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] b = new byte[1024];
            int n;
            while ((n = inputStream.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            byte[] data = bos.toByteArray();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private class ManifestTagVisitor extends NodeVisitor {
        public ManifestTagVisitor(NodeVisitor child) {
            super(child);
        }

        @Override
        public NodeVisitor child(String ns, String name) {
            NodeVisitor child = super.child(ns, name);
            if ("application".equals(name)) {
                return new ApplicationTagVisitor(child);
            }
            return child;
        }

        private class ApplicationTagVisitor extends NodeVisitor {
            public ApplicationTagVisitor(NodeVisitor child) {
                super(child);
            }

            @Override
            public NodeVisitor child(String ns, String name) {
                NodeVisitor child = super.child(ns, name);
                if("meta-data".equals(name)) {
                    return new MetaDataVisitor(child);
                }
                return child;
            }
        }
    }

    private class MetaDataVisitor extends NodeVisitor {
        public String name = null;
        public Object value = null;
        public MetaDataVisitor(NodeVisitor child) {
            super(child);
        }

        @Override
        public void attr(String ns, String name, int resourceId, int type, Object obj) {
            if (type == 3 && "name".equals(name)) {
                this.name = (String)obj;
            }
            if ("value".equals(name) ) {
                value = obj;
            }
            super.attr(ns, name, resourceId, type, obj);
        }

        @Override
        public void end() {
            if(name != null && value != null) {
                metaData.put(name, value);
            }
            super.end();
        }
    }
}
