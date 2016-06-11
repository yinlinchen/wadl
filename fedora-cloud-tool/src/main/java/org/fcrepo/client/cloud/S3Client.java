package org.fcrepo.client.cloud;

import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;

public class S3Client {

    public S3Client() {
    }

    public String getObjectContent(final FcrepoClient client, final URI url) {

        try {

            final FcrepoResponse res = client.get(url, "text", "texts");
            final InputStream is = res.getBody();
            final BufferedReader br = new BufferedReader(new InputStreamReader(is));
            final String content = org.apache.commons.io.IOUtils.toString(br);

            return content;

        } catch (final FcrepoOperationFailedException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public void createContainer(final FcrepoClient client, final URI url) {
        try {
            client.put(url, null, null);
        } catch (final FcrepoOperationFailedException e) {
            e.printStackTrace();
        }
    }

    public void deleteObject(final FcrepoClient client, final URI url) {

        try {
            client.delete(url);
            final String tombstoneurl = url.toString() + "/fcr:tombstone";
            client.delete(URI.create(tombstoneurl));
        } catch (final FcrepoOperationFailedException e) {
            e.printStackTrace();
        }

    }

    public void downloadFromCloud(final String provider, final String bucketname, final String objkey,
            final String filepath) {

        final String accesskeyid = System.getenv("AWS_ACCESS_KEY_ID");
        final String secretaccesskey = System.getenv("AWS_SECRET_ACCESS_KEY");

        // Initialize the BlobStoreContext
        final BlobStoreContext context = ContextBuilder.newBuilder(provider)
                .credentials(accesskeyid, secretaccesskey)
                .buildView(BlobStoreContext.class);

        final BlobStore blobStore = context.getBlobStore();

        final Blob tb = blobStore.getBlob(bucketname, objkey);

        try {

            final InputStream supplier = tb.getPayload().openStream();
            final OutputStream outStream = new FileOutputStream(new File(filepath));
            final byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = supplier.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            outStream.close();

        } catch (final IOException e1) {
            e1.printStackTrace();
        }
    }

    public static String downloadFromUrl(final URL url, final String localFilename) throws IOException {
        InputStream is = null;
        FileOutputStream fos = null;

        final String tempDir = System.getProperty("java.io.tmpdir");
        final String outputPath = tempDir + "/" + localFilename;

        try {
            // connect
            final URLConnection urlConn = url.openConnection();

            // get inputstream from connection
            is = urlConn.getInputStream();
            fos = new FileOutputStream(outputPath);

            // 4KB buffer
            final byte[] buffer = new byte[4096];
            int length;

            // read from source and write into local file
            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            return outputPath;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        }
    }

    public Hashtable<String, String> getCloudObjectInfo(final String provider, final String cloudurl) {

        final Hashtable<String, String> ht = new Hashtable<String, String>();
        final String accesskeyid = System.getenv("AWS_ACCESS_KEY_ID");
        final String secretaccesskey = System.getenv("AWS_SECRET_ACCESS_KEY");

        final BlobStoreContext context = ContextBuilder.newBuilder(provider)
                .credentials(accesskeyid, secretaccesskey)
                .buildView(BlobStoreContext.class);

        String bucketname = "";
        String objkey = "";
        if (provider.equals("aws-s3")) {
            final String[] obj = cloudurl.split("/");
            bucketname = obj[obj.length - 2].replace(".s3.amazonaws.com", "");
            objkey = obj[obj.length - 1];
        }
        ht.put("bucketname", bucketname);

        final BlobStore blobStore = context.getBlobStore();
        final BlobMetadata bm = blobStore.blobMetadata(bucketname, objkey);

        final String contentType = bm.getContentMetadata().getContentType();
        ht.put("mimetype", contentType);
        final String contentDisposition = bm.getContentMetadata().getContentDisposition();
        final String pattern = "filename=(.*)";
        final Pattern r = Pattern.compile(pattern);

        final Matcher m = r.matcher(contentDisposition);
        if (m.find()) {
            ht.put("filename", m.group(1));
        }

        context.close();
        return ht;

    }

    public Hashtable<String, String> getObjectInfo(final String content) {

        final Hashtable<String, String> ht = new Hashtable<String, String>();

        String pattern = "fedora:Binary";
        Pattern r = Pattern.compile(pattern);

        Matcher m = r.matcher(content);
        if (m.find()) {
            ht.put("type", m.group(0));
        } else {
            ht.put("type", "");
        }

        pattern = "fedora:hasParent <(.*)>";
        r = Pattern.compile(pattern);
        m = r.matcher(content);
        if (m.find()) {
            ht.put("parent", m.group(1));
        } else {
            ht.put("parent", "");
        }

        // get filename
        pattern = "ebucore:filename \"(.*)\"";
        r = Pattern.compile(pattern);
        m = r.matcher(content);
        if (m.find()) {
            ht.put("filename", m.group(1));
        } else {
            ht.put("filename", "");
        }

        // get hasMimeType
        pattern = "ebucore:hasMimeType \"(.*)\"";
        r = Pattern.compile(pattern);
        m = r.matcher(content);
        if (m.find()) {
            ht.put("mimetype", m.group(1));
        } else {
            ht.put("mimetype", "");
        }

        // get cloudurl
        pattern = "dc:source \"(.*)\"";
        r = Pattern.compile(pattern);
        m = r.matcher(content);
        if (m.find()) {
            ht.put("cloudurl", m.group(1));
        } else {
            ht.put("cloudurl", "");
        }

        return ht;

    }

    public String getLocalFile(final String urlstr, final String filename) {
        String filepath = null;

        try {

            final URL url = new URL(urlstr);
            filepath = S3Client.downloadFromUrl(url, filename);

        } catch (final IOException e) {
            e.printStackTrace();
        }

        return filepath;
    }

    public void ingestFileToFedora(final FcrepoClient client, final URI url, final String filepath,
            final String contentType) {

        final File initialFile = new File(filepath);

        try {
            final InputStream targetStream = new FileInputStream(initialFile);
            client.put(url, targetStream, contentType);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final FcrepoOperationFailedException e) {
            e.printStackTrace();
        }
    }

    public void updateContainer(final FcrepoClient client, final URI url, final String body,
            final String contentType) {

        final InputStream stream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));

        try {
            client.put(url, stream, contentType);
        } catch (final FcrepoOperationFailedException e) {
            e.printStackTrace();
        }

    }

    public URI uploadFileToS3(final String provider, final String bucketname,
            final Hashtable<String, String> objInfo,
            final String filepath) {

        final String accesskeyid = System.getenv("AWS_ACCESS_KEY_ID");
        final String secretaccesskey = System.getenv("AWS_SECRET_ACCESS_KEY");

        // Initialize the BlobStoreContext
        final BlobStoreContext context = ContextBuilder.newBuilder(provider)
                .credentials(accesskeyid, secretaccesskey)
                .buildView(BlobStoreContext.class);

        final URI s3url = null;

        // Access the BlobStore
        final BlobStore blobStore = context.getBlobStore();

        // final Create a Container
        blobStore.createContainerInLocation(null, bucketname);

        final ByteSource payload = Files.asByteSource(new File(filepath));
        Blob blob;
        try {
            blob = blobStore.blobBuilder(objInfo.get("filename"))
                    .payload(payload)
                    .contentDisposition("attachment; filename=" + objInfo.get("filename"))
                    .contentLength(payload.size())
                    .contentType(objInfo.get("mimetype"))
                    .build();

            final String eTag = blobStore.putBlob(bucketname, blob, multipart());

            if (eTag.length() > 0) {
                final String theUrl = "https://" + bucketname + ".s3.amazonaws.com/" + objInfo.get("filename");
                context.close();
                return URI.create(theUrl);
            }


        } catch (final IOException e) {
            e.printStackTrace();
        }

        context.close();
        return s3url;

    }

    /*
     * url: Fedora binary URL, end with /fcr:metadata
     */
    public void moveFileToCloud(final FcrepoClient client, final URI url, final String provider,
            final String buckname) {

        // get binary object content
        final String content = this.getObjectContent(client, url);

        // get object information. parent url, filename, mimetype
        final Hashtable<String, String> objInfo = this.getObjectInfo(content);

        final String fileurl = url.toString().replace("/fcr:metadata", "");
        String filename = objInfo.get("filename");
        if (filename.equals("")) {
            filename = fileurl.substring(fileurl.lastIndexOf("/") + 1, fileurl.length());
            objInfo.put("filename", filename);
        }

        // get local file
        final String filepath = this.getLocalFile(fileurl, filename);

        // upload file to s3
        final URI s3url = this.uploadFileToS3(provider, buckname, objInfo, filepath);

        // delete current obj
        this.deleteObject(client, url);

        // create new child obj
        this.createContainer(client, URI.create(fileurl));

        // update new child container
        final String newcontent = this.getObjectContent(client, URI.create(fileurl));
        final String updatebody = newcontent + " <> dc:source \"" + s3url.toString() + "\" .";
        this.updateContainer(client, URI.create(fileurl), updatebody, "text/turtle");

    }

    public void restoreFileToLocal(final FcrepoClient client, final URI url, final String provider) {

        // get binary object content
        final String content = this.getObjectContent(client, url);

        // get object information. parent url, filename, mimetype
        final Hashtable<String, String> objInfo = this.getObjectInfo(content);
        final String cloudurl = objInfo.get("cloudurl");

        final Hashtable<String, String> cloudObjInfo = this.getCloudObjectInfo(provider, cloudurl);

        // download S3 file
        final String tempDir = System.getProperty("java.io.tmpdir");
        final String filename = cloudObjInfo.get("filename");
        final String outputPath = tempDir + "/" + filename;
        this.downloadFromCloud(provider, cloudObjInfo.get("bucketname"), filename, outputPath);

        // delete current obj
        this.deleteObject(client, url);

        // ingest to container
        this.ingestFileToFedora(client, url, outputPath, cloudObjInfo.get("mimetype"));

    }

}
