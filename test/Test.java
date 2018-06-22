
import com.snal.model.util.text.TextUtil;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Luo Tao
 */
public class Test {

    public static void main(String[] args) {

        String list = TextUtil.readTxtFile("SR2018021200110022_HIVE_SYNC_TO_PAAS1.sql", false);
        System.out.println(list);
        String aa = "根据客户等级计算工单超时时长，统计当天超时时长超过大于零的工单数量，详细超时时间计算如下：\n"
                + "\n"
                + "1.五星钻、五星金 客户工单超时时长=（归档时间-受理时间） *24*3600 - 16*3600\n"
                + "2.五星普通、四星 客户工单超时时长=（归档时间-受理时间） *24*3600 - 24*3600\n"
                + "3.三星、二星、一星 客户工单超时时长=（归档时间-受理时间）*24*3600 -48*3600\n"
                + "4.准星、其他 客户工单超时时长=（归档时间-受理时间）*24*3600 -96*3600，如果超时时长小于零，则超时时长为零，其中客户等级参考TR_NGCC_DDIC中DATA_CD=104的数据字典定义。";

        Pattern p = Pattern.compile("(\r?\n(\\s*\r?\n)+)");
        Matcher m = p.matcher(list);
        list = m.replaceAll("\r\n");
        System.out.println(list);
    }
}
