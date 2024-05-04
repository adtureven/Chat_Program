import javax.sound.sampled.*;
import java.io.IOException;
import java.io.OutputStream;

public class test {
    public static void main(String[] args) {
//        AudioFormat format = new AudioFormat(
//                AudioFormat.Encoding.PCM_SIGNED, // 编码格式
//                44100,                           // 采样率
//                16,                              // 位深度
//                2,                               // 声道数（立体声）
//                4,                               // 每个样本帧字节数
//                44100,                           // 每秒的样本帧数
//                true);                           // 大端字节顺序（true表示大端，false表示小端）
        try {
            // 创建音频格式
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);

            // 获取音频输入设备
            TargetDataLine line = AudioSystem.getTargetDataLine(format);
            line.open(format);
            line.start();
            System.out.println("2222222222222222");
            line.stop();
            line.close();
        }catch (LineUnavailableException e) {
            e.printStackTrace();
        } finally {
            // 关闭音频输入设备和输出流

        }
    }
}

