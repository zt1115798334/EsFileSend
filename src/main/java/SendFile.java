import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created with IntelliJ IDEA.
 *
 * @author zhang
 * date: 2021/10/11 14:26
 * description:
 */
public class SendFile {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("read path：");
        String path = scanner.next();
        System.out.println("send url：");
        String url = scanner.next();

        File file1 = new File(path);
        ZipFile zfile = null;
        try {

            zfile = new ZipFile(file1, Charset.forName("GBK"));
            //断言函数即条件返回boolean类型的结果，如我想获取zip包中的某个文件，由于我的zip包中只有一个xml文件
            //所以正则写法如下。Predicate函数式接口，具体查看jdk文档。
            Predicate<? super ZipEntry> xml = ze -> ze.getName().matches(".*\\.txt");
            //Optional容器对象 ,结合Predicate获取到zip包中你所需要的文件，isPresent()判断是否存在对象，通过get()获取对象
            //过滤出你需要的文件，接下来具体操作根据需求
            List<? extends ZipEntry> zipEntries = zfile.stream().collect(Collectors.toList());
            int index = 0;
            int size = zipEntries.size();
            for (ZipEntry zipEntry : zipEntries) {
                index++;
                BufferedReader br = new BufferedReader(new InputStreamReader(zfile.getInputStream(zipEntry)));
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    HttpPost httpPost = new HttpPost(url);
                    List<BasicNameValuePair> pairs = new ArrayList<>();
                    pairs.add(new BasicNameValuePair("data", sb.toString()));
                    pairs.add(new BasicNameValuePair("fileName", String.valueOf(System.currentTimeMillis())));
                    httpPost.setEntity(new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8));
                    httpClient.execute(httpPost, httpResponse -> {
                        int status = httpResponse.getStatusLine().getStatusCode();
                        if (status < 200 || status >= 300) {
                            System.err.println("url exception status：" + status);
                        }
                        HttpEntity entity = httpResponse.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("size: " + size + " index:" + index + " speed of progress: " + percentage(size, index) + "%");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert zfile != null;
                zfile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String percentage(long total, long count) {
        BigDecimal totalBigDecimal = new BigDecimal(total);
        BigDecimal countBigDecimal = new BigDecimal(count);
        return countBigDecimal.divide(totalBigDecimal, 6, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100)).toString();
    }
}
