package org.example.redissonresourcelock;

import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class FormService {

    private final RedissonClient redissonClient;

    public FormService() {
        // 初始化 Redisson 客户端
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        this.redissonClient = Redisson.create(config);
    }

    /**
     * 用户上传图片
     *
     * @param formId   表单ID
     * @param userId   用户ID
     * @return 是否允许操作
     */
    public boolean uploadImage(String formId, String userId) {
        // 获取 Redis 中的 key
        RBucket<String> bucket = redissonClient.getBucket(formId);

        // 判断 key 是否存在
        if (bucket.isExists()) {
            // 如果 key 存在，判断 value 是否与当前用户ID相同
            String currentUserId = bucket.get();
            if (!currentUserId.equals(userId)) {
                // 如果不是当前用户，不允许操作
                return false;
            }
        } else {
            // 如果 key 不存在，允许操作，并设置 key-value 对
            bucket.set(userId, 60, java.util.concurrent.TimeUnit.SECONDS); // 设置过期时间，防止死锁
        }

        // 执行业务逻辑（上传图片）
        // TODO: 添加上传图片的业务逻辑

        return true;
    }

    /**
     * 用户保存表单
     *
     * @param formId   表单ID
     * @param userId   用户ID
     * @return 是否保存成功
     */
    public boolean saveForm(String formId, String userId) {
        // 获取 Redis 中的 key
        RBucket<String> bucket = redissonClient.getBucket(formId);

        // 判断 key 是否存在
        if (!bucket.isExists()) {
            // 如果 key 不存在，直接执行业务逻辑
            // TODO: 添加保存表单的业务逻辑
            return true;
        } else {
            // 如果 key 存在，判断 value 是否与当前用户ID相同
            String currentUserId = bucket.get();
            if (currentUserId.equals(userId)) {
                // 如果是当前用户，允许保存，并删除 Redis 数据
                bucket.delete();
                // TODO: 添加保存表单的业务逻辑
                return true;
            } else {
                // 如果不是当前用户，返回异常
                throw new RuntimeException("当前数据其他人正在操作");
            }
        }
    }

    /**
     * 关闭 Redisson 客户端
     */
    public void shutdown() {
        redissonClient.shutdown();
    }

    public static void main(String[] args) {
        FormService formService = new FormService();

        try {
            // 模拟用户1上传图片
            boolean uploadResult1 = formService.uploadImage("form_123", "user_1");
            System.out.println("用户1上传图片结果: " + uploadResult1);

            // 模拟用户2上传图片（应该失败）
            boolean uploadResult2 = formService.uploadImage("form_123", "user_2");
            System.out.println("用户2上传图片结果: " + uploadResult2);

            // 模拟用户1保存表单
            boolean saveResult1 = formService.saveForm("form_123", "user_1");
            System.out.println("用户1保存表单结果: " + saveResult1);

            // 模拟用户2保存表单（应该失败）
            boolean saveResult2 = formService.saveForm("form_123", "user_2");
            System.out.println("用户2保存表单结果: " + saveResult2);
        } catch (Exception e) {
            System.err.println("发生异常: " + e.getMessage());
        } finally {
            formService.shutdown();
        }
    }
}
