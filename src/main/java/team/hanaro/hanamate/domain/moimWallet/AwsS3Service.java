package team.hanaro.hanamate.domain.moimWallet;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import team.hanaro.hanamate.domain.moimWallet.repository.ImageRepository;
import team.hanaro.hanamate.entities.Article;
import team.hanaro.hanamate.entities.Images;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@PropertySource("application-S3.properties")
public class AwsS3Service {
    private final AmazonS3 amazonS3;
    private final ImageRepository imageRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${cloud.aws.region.static}")
    private String region;

    @Transactional
    public List<Images> uploadImage(List<MultipartFile> multipartFile, Article article) {
        List<Images> imagesList = new ArrayList<>();
        multipartFile.forEach(file -> {
            String fileName = createFileName(file.getOriginalFilename());
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());
            try (InputStream inputStream = file.getInputStream()) {
                amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead));
                //실제 파일 경로를 만들어낸다
                String fileUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + fileName;
                //이미지 테이블에 정보를 저장한다.
                Images image = Images.builder()
                        .article(article)
                        .fileName(file.getOriginalFilename())
                        .savedName(fileName)
                        .savedPath(fileUrl)
                        .fileSize(objectMetadata.getContentLength())
                        .fullFileType(file.getContentType())
                        .build();
                imagesList.add(imageRepository.save(image));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다.");
            }
        });
        return imagesList;
    }

    public void deleteImage(String fileName) {
        amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
    }

    //파일 이름을 UUID + 확장자 명으로 합쳐서 반환한다.
    private String createFileName(String fileName) {
        return UUID.randomUUID().toString().concat(getFileExtension(fileName));
    }

    //뒤에서부터 " . " 을 찾는다음에 그 다음문자를 substring 해서 확장자를 추출한다.
    //.이 붙은 확장자가 없으면 잘못된 파일로 판단하고 예외처리
    private String getFileExtension2 (String fileName) {
        try {
            return fileName.substring(fileName.lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 형식의 파일(" + fileName + ") 입니다.");
        }
    }

    private String getFileExtension(String fileName) {
        try {
            String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
            List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif");
            // 이미지 확장자인지 확인
            if (!allowedExtensions.contains(extension)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일(.jpg, .jpeg, .png, .gif)만 업로드할 수 있습니다.");
            }
            return extension;
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 형식의 파일(" + fileName + ") 입니다.");
        }
    }

}
