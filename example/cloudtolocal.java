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

        final String urlstr = "http://localhost:8080/rest/S2/concept.png";
        final URI url = URI.create(urlstr);
        final String provider = "aws-s3";

        s3cli.restoreFileToLocal(client, url, provider);

    }
}
