package test.test1;

import java.net.URI;

import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.cloud.S3Client;

public class localtocloud
{
    public static void main( final String[] args )
    {
        
        final S3Client s3cli = new S3Client();
        // Fedora 4 server URL: http://localhost:8080/rest/
        final FcrepoClient client = new FcrepoClient("", "", "localhost", true);

        final String urlstr = "http://localhost:8080/rest/node/to/move/fcr:metadata";
        final URI url = URI.create(urlstr);
        final String provider = "aws-s3";
        final String buckname = "s3bucket";

        s3cli.moveFileToCloud(client, url, provider, buckname);

    }
}
