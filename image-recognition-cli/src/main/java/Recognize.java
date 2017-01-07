import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by leon on 12/17/16.
 */
public class Recognize {
    public static final String S3_BUCKET = "leonoosterwijk-image-recognition";
    public static final String imgToSubmit = "/tmp/image_recognition.jpg";
    private static final Float CONFIDENCE_THRESHOLD = 80.0f;

    public static enum People {
        ADINA("adina"), RAVI("ravi"), BLISS("bliss"), ATTICUS("atticus");
        String bucket;

        People(String bucket) {
            this.bucket = bucket;
        }
    }

    public static void main(String[] args) throws Exception {
        AWSCredentials credentials;
        String filename = args[0];
        try {
            credentials = new ProfileCredentialsProvider("default").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
                    + "Please make sure that your credentials file is at the correct "
                    + "location (/Users/<userid>/.aws/credentials), and is in valid format.", e);
        }


        copy(filename, imgToSubmit);


        // /Users/leon/Documents/dev/raspberry
        AmazonS3 s3 = new AmazonS3Client(credentials);
        Region usEast = Region.getRegion(Regions.US_EAST_1);
        s3.setRegion(usEast);
        PutObjectResult putObjectResult = s3.putObject(new PutObjectRequest(S3_BUCKET, "image_class.jpg", candidateImage(imgToSubmit)));
        //System.out.println("Uploaded: " + putObjectResult.getContentMd5());

        AmazonRekognitionClient rekognitionClient = new AmazonRekognitionClient(credentials);
//                .withEndpoint("service-endpoint");
        rekognitionClient.setSignerRegionOverride("us-east-1");
        ObjectMapper objectMapper = new ObjectMapper();
        Image source = getImageUtil(S3_BUCKET, "image_class.jpg");
        //Image source = getImageUtil(S3_BUCKET, "atticus/atticus5.jpg");


        DetectLabelsRequest request = new DetectLabelsRequest()
                .withImage(source)
                .withMaxLabels(10)
                .withMinConfidence(77F);
        try {
            DetectLabelsResult result = rekognitionClient.detectLabels(request);
            String stuff = result.getLabels().stream().filter(e -> e.getConfidence()> CONFIDENCE_THRESHOLD
            ).map(e -> e.getName()).collect(Collectors.joining(", "));;
            System.out.print("I see " + stuff + ". ");

        } catch (AmazonRekognitionException e) {
            e.printStackTrace();
        }


        DetectFacesRequest detectFacesRequest = new DetectFacesRequest()
                .withImage(source)
                .withAttributes(Attribute.ALL);
        DetectFacesResult result = null;
        try {
            result = rekognitionClient.detectFaces(detectFacesRequest);
        } catch (AmazonRekognitionException e) {
            e.printStackTrace();
        }

        if (result.getFaceDetails().size() > 0) {
            detectFaces(rekognitionClient, source);
        } else {
            System.out.println("I did not see any people though.");
        }


//        System.out.println(objectMapper.writeValueAsString(compareFacesResult));
    }

    private static void detectFaces(AmazonRekognitionClient rekognitionClient, Image source) {
        Float similarityThreshold = 70F;
        People classificationResult = null;
        outerloop:
        for (int i = 1; i < 5; i++) {
            for (People p : People.values()) {
                Image target = getImageUtil(S3_BUCKET, p.bucket + "/" + p.bucket + "" + i + ".jpg");
                System.err.println("Comparing to" + target.toString());
                CompareFacesResult compareFacesResult = callCompareFaces(source, target, similarityThreshold, rekognitionClient);
                //Float similarity =
                Optional<CompareFacesMatch> first = compareFacesResult.getFaceMatches().stream().findFirst();
                if (first.isPresent()) {
                    if (first.get().getSimilarity() > similarityThreshold) {
                        System.err.println(p.bucket + " - " + i + " = " + first.get().getSimilarity());
                        classificationResult = p;
                        break outerloop;
                    }
                }
            }
        }
        if (classificationResult != null) {
            System.out.println("I also see " + classificationResult.name().toLowerCase() + ". Hello "+ classificationResult.name().toLowerCase() +".");
        } else {
            System.out.println("I also see a person I did not recognize.");
        }
    }

    private static CompareFacesResult callCompareFaces(Image sourceImage, Image targetImage,
                                                       Float similarityThreshold, AmazonRekognition amazonRekognition) {
        CompareFacesRequest compareFacesRequest = new CompareFacesRequest()
                .withSourceImage(sourceImage)
                .withTargetImage(targetImage)
                .withSimilarityThreshold(similarityThreshold);
        return amazonRekognition.compareFaces(compareFacesRequest);
    }

    private static Image getImageUtil(String bucket, String key) {
        return new Image()
                .withS3Object(new S3Object()
                        .withBucket(bucket)
                        .withName(key));
    }

    private static File candidateImage(String filename) {
        //return new File("/Users/leon/Documents/dev/raspberry/cam2.jpg");
        //return new File("/dev/shm/mjpeg/cam.jpg");
        return new File(filename);

    }

    private static void copy(String from, String to) {
        InputStream inStream = null;
        OutputStream outStream = null;

        try {

            File afile = new File(from);
            File bfile = new File(to);

            inStream = new FileInputStream(afile);
            outStream = new FileOutputStream(bfile);

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            while ((length = inStream.read(buffer)) > 0) {

                outStream.write(buffer, 0, length);

            }

            inStream.close();
            outStream.close();

            //System.out.println("File is copied successful!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
