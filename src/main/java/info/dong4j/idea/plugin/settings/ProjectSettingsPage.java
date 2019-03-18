package info.dong4j.idea.plugin.settings;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;

import info.dong4j.idea.plugin.enums.ImageMarkEnum;
import info.dong4j.idea.plugin.enums.SuffixEnum;
import info.dong4j.idea.plugin.singleton.AliyunOssClient;
import info.dong4j.idea.plugin.util.DES;
import info.dong4j.idea.plugin.util.EnumsUtils;
import info.dong4j.idea.plugin.util.UploadUtils;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionListener;
import java.io.*;
import java.util.Objects;
import java.util.Optional;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Company: 科大讯飞股份有限公司-四川分公司</p>
 * <p>Description: </p>
 *
 * @author dong4j
 * @date 2019 -03-13 12:19
 * @email sjdong3 @iflytek.com
 */
@Slf4j
public class ProjectSettingsPage implements SearchableConfigurable, Configurable.NoScroll {
    private OssPersistenConfig ossPersistenConfig;
    private static final String TEST_FILE_NAME = "test.png";

    private JPanel myMainPanel;

    private JTabbedPane authorizationTabbedPanel;
    /** weiboOssAuthorizationPanel group */
    private JPanel weiboOssAuthorizationPanel;
    private JTextField weiboUserNameTextField;
    private JPasswordField weiboPasswordField;
    private JLabel userNameLabel;
    private JLabel passwordLabel;
    /** aliyunOssAuthorizationPanel group */
    private JPanel aliyunOssAuthorizationPanel;
    private JTextField aliyunOssBucketNameTextField;
    private JTextField aliyunOssAccessKeyTextField;
    private JPasswordField aliyunOssAccessSecretKeyTextField;
    private JTextField aliyunOssEndpointTextField;
    private JTextField aliyunOssFileDirTextField;
    private JComboBox aliyunOssSuffixBoxField;
    private JTextField exampleTextField;
    /** qiniuOssAuthorizationPanel group */
    private JPanel qiniuOssAuthorizationPanel;
    private JTextField qiniuOssBucketNameTextField;
    private JTextField qiniuOssAccessKeyTextField;
    private JTextField qiniuOssAccessSecretKeyTextField;
    private JTextField qiniuOssUpHostTextField;
    private JRadioButton qiniuOssEastChinaRadioButton;
    private JRadioButton qiniuOssNortChinaRadioButton;
    private JRadioButton qiniuOssSouthChinaRadioButton;
    private JRadioButton qiniuOssNorthAmeriaRadioButton;
    /** globalUploadPanel group */
    private JPanel globalUploadPanel;
    private JCheckBox changeToHtmlTagCheckBox;
    private JRadioButton largePictureRadioButton;
    private JRadioButton commonRadioButton;
    private JLabel commonLabel;
    private JRadioButton customRadioButton;
    private JLabel customLabel;
    private JTextField customHtmlTypeTextField;
    private JCheckBox compressCheckBox;
    private JCheckBox compressBeforeUploadCheckBox;
    private JCheckBox compressAtLookupCheckBox;
    private JSlider compressSlider;
    private JLabel compressLabel;
    private JTextField styleNameTextField;
    private JCheckBox transportCheckBox;
    private JCheckBox backupCheckBox;

    /** 按钮 group */
    private JButton testButton;
    private JLabel testMessage;
    private JButton helpButton;

    private JPanel clipboardPanel;
    /** clipboard group */
    private JCheckBox clipboardControlCheckBox;
    private JCheckBox copyToDirCheckBox;
    private JTextField whereToCopyTextField;
    private JCheckBox uploadAndReplaceCheckBox;
    private JComboBox defaultCloudComboBox;

    public ProjectSettingsPage() {
        log.trace("ProjectSettingsPage Constructor invoke");
        ossPersistenConfig = OssPersistenConfig.getInstance();
        if (ossPersistenConfig != null) {
            reset();
        }
    }

    @Nls
    @Override
    public String getDisplayName() {
        log.trace("get plugin setting DisplayName");
        return "Aliyun OSS Settings";
    }

    @Override
    public JComponent createComponent() {
        log.trace("createComponent");
        initFromSettings();
        return myMainPanel;
    }

    /**
     * 每次打开设置面板时执行
     */
    private void initFromSettings() {
        initAuthorizationTabbedPanel();
        initUploadPanel();
        initClipboardControl();
    }

    /**
     * 初始化 clipboard group
     */
    private void initClipboardControl() {
        // 设置是否勾选
        boolean isClipboardControl = ossPersistenConfig.getState().isClipboardControl();
        boolean isCopyToDir = ossPersistenConfig.getState().isCopyToDir();
        boolean isUploadAndReplace = ossPersistenConfig.getState().isUploadAndReplace();
        this.clipboardControlCheckBox.setSelected(isClipboardControl);
        this.copyToDirCheckBox.setSelected(isCopyToDir);
        this.uploadAndReplaceCheckBox.setSelected(isUploadAndReplace);

        // 设置是否可用
        this.copyToDirCheckBox.setEnabled(isClipboardControl);
        this.uploadAndReplaceCheckBox.setEnabled(isClipboardControl);
        // 设置 copy 位置
        this.whereToCopyTextField.setText(ossPersistenConfig.getState().getImageSavePath());
        this.whereToCopyTextField.setEnabled(isClipboardControl && isCopyToDir);
        // 默认上传图床
        this.defaultCloudComboBox.setEnabled(isUploadAndReplace && isClipboardControl);
        this.defaultCloudComboBox.setSelectedIndex(ossPersistenConfig.getState().getCloudType());

        // 设置 clipboardControlCheckBox 监听
        clipboardControlCheckBox.addChangeListener(e -> {
            JCheckBox checkBox = (JCheckBox) e.getSource();
            copyToDirCheckBox.setEnabled(checkBox.isSelected());
            uploadAndReplaceCheckBox.setEnabled(checkBox.isSelected());
            // 如果都被选中才设置为可用
            whereToCopyTextField.setEnabled(copyToDirCheckBox.isSelected() && checkBox.isSelected());
            defaultCloudComboBox.setEnabled(uploadAndReplaceCheckBox.isSelected() && checkBox.isSelected());
        });

        // 设置 copyToDirCheckBox 监听
        copyToDirCheckBox.addChangeListener(e -> {
            JCheckBox checkBox = (JCheckBox) e.getSource();
            whereToCopyTextField.setEnabled(checkBox.isSelected());
        });

        // 设置 uploadAndReplaceCheckBox 监听
        uploadAndReplaceCheckBox.addChangeListener(e -> {
            JCheckBox checkBox = (JCheckBox) e.getSource();
            defaultCloudComboBox.setEnabled(checkBox.isSelected());
        });
    }

    /**
     * 初始化 authorizationTabbedPanel group
     */
    private void initAuthorizationTabbedPanel() {
        // 打开设置页时默认选中默认上传图床
        authorizationTabbedPanel.setSelectedIndex(ossPersistenConfig.getState().getCloudType());
        authorizationTabbedPanel.addChangeListener(e -> {
            // 清理 test 信息
            testMessage.setText("");
            // 获得指定索引的选项卡标签
            log.trace("change {}", authorizationTabbedPanel.getTitleAt(authorizationTabbedPanel.getSelectedIndex()));
        });

        initAliyunOssAuthenticationPanel();
        initWeiboOssAuthenticationPanel();
        initQiniuOssAuthenticationPanel();
        testAndHelpListener();
    }

    /**
     * 初始化 aliyun oss 认证相关设置
     */
    private void initAliyunOssAuthenticationPanel() {
        String aliyunOssAccessSecretKey = ossPersistenConfig.getState().getAliyunOssState().getAccessSecretKey();
        aliyunOssAccessSecretKeyTextField.setText(DES.decrypt(aliyunOssAccessSecretKey, OssState.ALIYUN));

        // 根据持久化配置设置为被选中的 item
        aliyunOssSuffixBoxField.setSelectedItem(ossPersistenConfig.getState().getAliyunOssState().getSuffix());
        // 处理当 aliyunOssFileDirTextField.getText() 为 空字符时, 不拼接 "/
        setExampleText();

        // 监听 aliyunOssBucketNameTextField
        aliyunOssBucketNameTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                setExampleText();
            }
        });

        // 监听 aliyunOssEndpointTextField
        aliyunOssEndpointTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                setExampleText();
            }
        });

        // 设置 aliyunOssFileDirTextField 输入的监听
        aliyunOssFileDirTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                setExampleText();
            }
        });

        // 添加监听器, 当选项被修改后, 修改 exampleTextField 中的 text
        aliyunOssSuffixBoxField.addItemListener(e -> {
            setExampleText();
        });
    }

    /**
     * 初始化 weibo oss 认证相关设置
     */
    private void initWeiboOssAuthenticationPanel() {
        weiboUserNameTextField.setText(ossPersistenConfig.getState().getWeiboOssState().getUserName());
        weiboPasswordField.setText(DES.decrypt(ossPersistenConfig.getState().getWeiboOssState().getPassword(), OssState.WEIBOKEY));
    }

    /**
     * 初始化 qiniu oss 认证相关设置
     */
    private void initQiniuOssAuthenticationPanel() {

    }

    /**
     * 添加 test 和 help 按钮监听, 根据选中的图床进行测试
     */
    private void testAndHelpListener() {
        // "Test" 按钮点击事件处理
        testButton.addActionListener(e -> {
            int index = authorizationTabbedPanel.getSelectedIndex();
            InputStream is = this.getClass().getResourceAsStream("/" + TEST_FILE_NAME);
            String url = UploadUtils.upload(EnumsUtils.getCloudEnum(index), is, TEST_FILE_NAME, (JPanel) authorizationTabbedPanel.getComponentAt(index));
            if (StringUtils.isNotBlank(url)) {
                testMessage.setForeground(JBColor.GREEN);
                testMessage.setText("Upload Succeed");
                if (log.isTraceEnabled()) {
                    BrowserUtil.browse(url);
                }
            } else {
                testMessage.setForeground(JBColor.RED);
                testMessage.setText("Upload Failed, Please check the configuration");
            }
        });

        // help button 监听
        helpButton.addActionListener(e -> {
            // 打开浏览器到帮助页面
            String url = "http://dong4j.info";
            BrowserUtil.browse(url);
        });
    }

    /**
     * 实时更新此字段
     */
    private void setExampleText() {
        String fileDir = StringUtils.isBlank(aliyunOssFileDirTextField.getText().trim()) ? "" : "/" + aliyunOssFileDirTextField.getText().trim();
        String endpoint = aliyunOssEndpointTextField.getText().trim();
        String backetName = aliyunOssBucketNameTextField.getText().trim();
        String url = AliyunOssClient.URL_PROTOCOL_HTTPS + "://" + backetName + "." + endpoint;
        exampleTextField.setText(url + fileDir + getSufixString(Objects.requireNonNull(aliyunOssSuffixBoxField.getSelectedItem()).toString()));
    }

    /**
     * 初始化 upload 配置组
     */
    private void initUploadPanel() {
        initChangeToHtmlGroup();
        initCompressGroup();
        initExpandGroup();
    }

    /**
     * 初始化图片备份和图床迁移
     */
    private void initExpandGroup() {
        // todo-dong4j : (2019年03月15日 20:52) [删除此设置, 使用 MoveToOtherStorageAction 替代]
        this.transportCheckBox.setSelected(ossPersistenConfig.getState().isTransport());
        this.backupCheckBox.setSelected(ossPersistenConfig.getState().isBackup());
    }

    /**
     * 初始化图片压缩配置组
     */
    private void initCompressGroup() {
        styleNameTextField.addFocusListener(new JTextFieldHintListener(styleNameTextField, "请提前在 Aliyun OSS 控制台设置"));

        boolean compressStatus = ossPersistenConfig.getState().isCompress();
        boolean beforeCompressStatus = ossPersistenConfig.getState().isCompressBeforeUpload();
        boolean lookUpCompressStatus = ossPersistenConfig.getState().isCompressAtLookup();
        // 设置被选中
        this.compressCheckBox.setSelected(compressStatus);
        // 设置组下多选框状态
        this.compressBeforeUploadCheckBox.setEnabled(compressStatus);
        this.compressBeforeUploadCheckBox.setSelected(beforeCompressStatus);
        this.compressAtLookupCheckBox.setEnabled(compressStatus);
        this.compressAtLookupCheckBox.setSelected(lookUpCompressStatus);

        this.compressSlider.setEnabled(compressStatus && beforeCompressStatus);
        this.compressSlider.setValue(ossPersistenConfig.getState().getCompressBeforeUploadOfPercent());

        // 设置主刻度间隔
        compressSlider.setMajorTickSpacing(10);
        // 设置次刻度间隔
        compressSlider.setMinorTickSpacing(2);
        // 绘制 刻度 和 标签
        compressSlider.setPaintTicks(true);
        compressSlider.setPaintLabels(true);
        compressSlider.addChangeListener(e -> compressLabel.setText(String.valueOf(compressSlider.getValue())));

        this.compressLabel.setText(String.valueOf(compressSlider.getValue()));
        this.styleNameTextField.setEnabled(compressStatus && lookUpCompressStatus);
        this.styleNameTextField.setText(ossPersistenConfig.getState().getStyleName());

        // compressCheckBox 监听, 修改组下组件状态
        compressCheckBox.addChangeListener(e -> {
            JCheckBox checkBox = (JCheckBox) e.getSource();
            if (checkBox.isSelected()) {
                compressBeforeUploadCheckBox.setEnabled(true);
                compressAtLookupCheckBox.setEnabled(true);
            } else {
                compressBeforeUploadCheckBox.setEnabled(false);
                compressAtLookupCheckBox.setEnabled(false);
                compressSlider.setEnabled(false);
                styleNameTextField.setEnabled(false);
            }
        });

        compressBeforeUploadCheckBox.addChangeListener(e -> {
            JCheckBox checkBox = (JCheckBox) e.getSource();
            compressSlider.setEnabled(checkBox.isSelected());
        });
        compressAtLookupCheckBox.addChangeListener(e -> {
            JCheckBox checkBox = (JCheckBox) e.getSource();
            styleNameTextField.setEnabled(checkBox.isSelected());
        });
    }

    /**
     * 初始化替换标签设置组
     */
    private void initChangeToHtmlGroup() {
        customHtmlTypeTextField.addFocusListener(new JTextFieldHintListener(customHtmlTypeTextField, "格式: <a title='${}' href='${}' >![${}](${})</a>"));

        // 初始化 changeToHtmlTagCheckBox 选中状态
        // 设置被选中
        boolean changeToHtmlTagCheckBoxStatus = ossPersistenConfig.getState().isChangeToHtmlTag();

        this.changeToHtmlTagCheckBox.setSelected(changeToHtmlTagCheckBoxStatus);
        // 设置组下单选框可用
        this.largePictureRadioButton.setEnabled(changeToHtmlTagCheckBoxStatus);
        this.commonRadioButton.setEnabled(changeToHtmlTagCheckBoxStatus);
        this.customRadioButton.setEnabled(changeToHtmlTagCheckBoxStatus);

        // 初始化 changeToHtmlTagCheckBox 组下单选框状态
        if (ImageMarkEnum.COMMON_PICTURE.text.equals(ossPersistenConfig.getState().getTagType())) {
            commonRadioButton.setSelected(true);
        } else if (ImageMarkEnum.LARGE_PICTURE.text.equals(ossPersistenConfig.getState().getTagType())) {
            largePictureRadioButton.setSelected(true);
        } else if (ImageMarkEnum.CUSTOM.text.equals(ossPersistenConfig.getState().getTagType())) {
            customRadioButton.setSelected(true);
            customHtmlTypeTextField.setEnabled(changeToHtmlTagCheckBoxStatus);
            customHtmlTypeTextField.setText(ossPersistenConfig.getState().getTagTypeCode());
        } else {
            commonRadioButton.setSelected(true);
        }

        // changeToHtmlTagCheckBox 监听, 修改组下组件状态
        changeToHtmlTagCheckBox.addChangeListener(e -> {
            JCheckBox checkBox = (JCheckBox) e.getSource();
            largePictureRadioButton.setEnabled(checkBox.isSelected());
            commonRadioButton.setEnabled(checkBox.isSelected());
            customRadioButton.setEnabled(checkBox.isSelected());
            // 如果原来自定义选项被选中, 则将输入框设置为可用
            if (customRadioButton.isSelected() && checkBox.isSelected()) {
                customHtmlTypeTextField.setEnabled(true);
            }
        });
        ButtonGroup group = new ButtonGroup();
        addRadioButton(group, largePictureRadioButton);
        addRadioButton(group, commonRadioButton);
        addRadioButton(group, customRadioButton);
    }

    /**
     * 处理被选中的单选框
     *
     * @param group  the group
     * @param button the button
     */
    private void addRadioButton(ButtonGroup group, JRadioButton button) {
        // 设置name即为 actionCommand
        group.add(button);
        // 构造一个监听器，响应checkBox事件
        ActionListener actionListener = e -> {
            Object sourceObject = e.getSource();
            if (sourceObject instanceof JRadioButton) {
                JRadioButton sourceButton = (JRadioButton) sourceObject;
                if (ImageMarkEnum.CUSTOM.text.equals(sourceButton.getText())) {
                    customHtmlTypeTextField.setEnabled(true);
                } else {
                    customHtmlTypeTextField.setEnabled(false);
                }
            }
        };
        button.addActionListener(actionListener);
    }

    /**
     * 生成文件名后缀
     *
     * @param name the name
     * @return the sufix string
     */
    @NotNull
    private String getSufixString(String name) {
        if (SuffixEnum.FILE_NAME.name.equals(name)) {
            return "/image.png";
        } else if (SuffixEnum.DATE_FILE_NAME.name.equals(name)) {
            return "/2019-2-30-image.png";
        } else if (SuffixEnum.RANDOM.name.equals(name)) {
            return "/98knb.png";
        } else {
            return "";
        }
    }

    /**
     * 判断 GUI 是否有变化
     *
     * @return the boolean
     */
    @Override
    public boolean isModified() {
        log.trace("isModified invoke");
        OssState state = ossPersistenConfig.getState();
        return !(isAliyunAuthModified(state)
                 && isWeiboAuthModified(state)
                 && isGeneralModified(state)
                 && isClipboardModified(state)
        );
    }

    private boolean isAliyunAuthModified(@NotNull OssState state) {
        OssState.AliyunOssState aliyunOssState = state.getAliyunOssState();
        String newBucketName = aliyunOssBucketNameTextField.getText().trim();
        String newAccessKey = aliyunOssAccessKeyTextField.getText().trim();
        String newAccessSecretKey = new String(aliyunOssAccessSecretKeyTextField.getPassword());
        if (StringUtils.isNotBlank(newAccessSecretKey)) {
            newAccessSecretKey = DES.encrypt(newAccessSecretKey, OssState.ALIYUN);
        }
        String newEndpoint = aliyunOssEndpointTextField.getText().trim();
        String newFileDir = aliyunOssFileDirTextField.getText().trim();
        String newSuffix = "";

        int index = aliyunOssSuffixBoxField.getSelectedIndex();
        Optional<SuffixEnum> type = EnumsUtils.getEnumObject(SuffixEnum.class, e -> e.getIndex() == index);
        if (type.isPresent()) {
            newSuffix = type.get().getName();
        }

        return newBucketName.equals(aliyunOssState.getBucketName())
               && newAccessKey.equals(aliyunOssState.getAccessKey())
               && newAccessSecretKey.equals(aliyunOssState.getAccessSecretKey())
               && newEndpoint.equals(aliyunOssState.getEndpoint())
               && newFileDir.equals(aliyunOssState.getFiledir())
               && newSuffix.equals(aliyunOssState.getSuffix());
    }

    private boolean isWeiboAuthModified(@NotNull OssState state) {
        OssState.WeiboOssState weiboOssState = state.getWeiboOssState();
        String weiboUsername = weiboUserNameTextField.getText().trim();
        String weiboPassword = new String(weiboPasswordField.getPassword());
        if (StringUtils.isNotBlank(weiboPassword)) {
            weiboPassword = DES.encrypt(weiboPassword, OssState.WEIBOKEY);
        }
        return weiboUsername.equals(weiboOssState.getUserName())
               && weiboPassword.equals(weiboOssState.getPassword());
    }

    private boolean isGeneralModified(OssState state) {
        // 是否替换标签
        boolean changeToHtmlTag = changeToHtmlTagCheckBox.isSelected();
        // 替换的标签类型
        String tagType = "";
        // 替换的标签类型 code
        String tagTypeCode = "";
        if (changeToHtmlTag) {
            // 正常的
            if (commonRadioButton.isSelected()) {
                tagType = ImageMarkEnum.COMMON_PICTURE.text;
                tagTypeCode = ImageMarkEnum.COMMON_PICTURE.code;
            }
            // 点击看大图
            else if (largePictureRadioButton.isSelected()) {
                tagType = ImageMarkEnum.LARGE_PICTURE.text;
                tagTypeCode = ImageMarkEnum.LARGE_PICTURE.code;
            }
            // 自定义
            else if (customRadioButton.isSelected()) {
                tagType = ImageMarkEnum.CUSTOM.text;
                // todo-dong4j : (2019年03月14日 14:30) [格式验证]
                tagTypeCode = customHtmlTypeTextField.getText().trim();
            }
        }

        // 是否压缩图片
        boolean compress = compressCheckBox.isSelected();
        // 上传前压缩
        boolean compressBeforeUpload = compressBeforeUploadCheckBox.isSelected();
        // 压缩比例
        int compressBeforeUploadOfPercent = compressSlider.getValue();
        // 查看时压缩
        boolean compressAtLookup = compressAtLookupCheckBox.isSelected();
        // Aliyun OSS 图片压缩配置
        String styleName = "";
        if (compressAtLookup) {
            styleName = styleNameTextField.getText().trim();
        }
        // 图床迁移
        boolean transport = transportCheckBox.isSelected();
        // 图片备份
        boolean backup = backupCheckBox.isSelected();

        return changeToHtmlTag == state.isChangeToHtmlTag()
               && tagType.equals(state.getTagType())
               && tagTypeCode.equals(state.getTagTypeCode())
               && compress == state.isCompress()
               && compressBeforeUpload == state.isCompressBeforeUpload()
               && compressBeforeUploadOfPercent == state.getCompressBeforeUploadOfPercent()
               && compressAtLookup == state.isCompressAtLookup()
               && styleName.equals(state.getStyleName())
               && transport == state.isTransport()
               && backup == state.isBackup();
    }

    private boolean isClipboardModified(@NotNull OssState state) {
        boolean clipboardControl = clipboardControlCheckBox.isSelected();
        boolean copyToDir = copyToDirCheckBox.isSelected();
        boolean uploadAndReplace = uploadAndReplaceCheckBox.isSelected();
        String whereToCopy = whereToCopyTextField.getText().trim();

        return clipboardControl == state.isClipboardControl()
               && copyToDir == state.isCopyToDir()
               && uploadAndReplace == state.isUploadAndReplace()
               && whereToCopy.equals(state.getImageSavePath())
               && defaultCloudComboBox.getSelectedIndex() == state.getCloudType();
    }

    /**
     * 配置被修改后时被调用, 修改 state 中的数据
     */
    @Override
    public void apply() {
        log.trace("apply invoke");
        OssState state = ossPersistenConfig.getState();
        applyAliyunAuthConfigs(state);
        applyGeneralConfigs(state);
        applyClipboardConfigs(state);
        applyWeiboAuthConfigs(state);
    }

    private void applyAliyunAuthConfigs(OssState state) {
        OssState.AliyunOssState aliyunOssState = state.getAliyunOssState();
        String bucketName = this.aliyunOssBucketNameTextField.getText().trim();
        String accessKey = this.aliyunOssAccessKeyTextField.getText().trim();
        String accessSecretKey = new String(aliyunOssAccessSecretKeyTextField.getPassword());
        String endpoint = this.aliyunOssEndpointTextField.getText().trim();
        // 需要在加密之前计算 hashcode
        int hashcode = bucketName.hashCode() +
                       accessKey.hashCode() +
                       accessSecretKey.hashCode() +
                       endpoint.hashCode();
        aliyunOssState.getOldAndNewAuthInfo().put(OssState.NEW_HASH_KEY, String.valueOf(hashcode));
        if (StringUtils.isNotBlank(accessSecretKey)) {
            accessSecretKey = DES.encrypt(accessSecretKey, OssState.ALIYUN);
        }

        aliyunOssState.setBucketName(bucketName);
        aliyunOssState.setAccessKey(accessKey);
        aliyunOssState.setAccessSecretKey(accessSecretKey);
        aliyunOssState.setEndpoint(endpoint);
        aliyunOssState.setFiledir(this.aliyunOssFileDirTextField.getText().trim());
        aliyunOssState.setSuffix(Objects.requireNonNull(this.aliyunOssSuffixBoxField.getSelectedItem()).toString());
    }

    private void applyWeiboAuthConfigs(OssState state) {
        OssState.WeiboOssState weiboOssState = state.getWeiboOssState();
        // 处理 weibo 保存时的逻辑 (保存之前必须通过测试, 右键菜单才可用)
        String username = this.weiboUserNameTextField.getText().trim();
        String password = new String(weiboPasswordField.getPassword());
        // 需要在加密之前计算 hashcode
        int hashcode = username.hashCode() + password.hashCode();
        weiboOssState.getOldAndNewAuthInfo().put(OssState.NEW_HASH_KEY, String.valueOf(hashcode));
        if (StringUtils.isNotBlank(password)) {
            password = DES.encrypt(password, OssState.WEIBOKEY);
        }

        weiboOssState.setUserName(username);
        weiboOssState.setPassword(password);
    }

    private void applyGeneralConfigs(OssState state) {
        state.setChangeToHtmlTag(this.changeToHtmlTagCheckBox.isSelected());
        if (this.changeToHtmlTagCheckBox.isSelected()) {
            // 正常的
            if (commonRadioButton.isSelected()) {
                state.setTagType(ImageMarkEnum.COMMON_PICTURE.text);
                state.setTagTypeCode(ImageMarkEnum.COMMON_PICTURE.code);
            }
            // 点击看大图
            else if (largePictureRadioButton.isSelected()) {
                state.setTagType(ImageMarkEnum.LARGE_PICTURE.text);
                state.setTagTypeCode(ImageMarkEnum.LARGE_PICTURE.code);
            }
            // 自定义
            else if (customRadioButton.isSelected()) {
                state.setTagType(ImageMarkEnum.CUSTOM.text);
                // todo-dong4j : (2019年03月14日 14:30) [格式验证]
                state.setTagTypeCode(customHtmlTypeTextField.getText().trim());
            }
        }
        state.setCompress(this.compressCheckBox.isSelected());
        state.setCompressBeforeUpload(this.compressBeforeUploadCheckBox.isSelected());
        state.setCompressBeforeUploadOfPercent(this.compressSlider.getValue());
        state.setCompressAtLookup(this.compressAtLookupCheckBox.isSelected());
        state.setStyleName(this.styleNameTextField.getText().trim());
        state.setTransport(this.transportCheckBox.isSelected());
        state.setBackup(this.backupCheckBox.isSelected());
    }

    private void applyClipboardConfigs(OssState state) {
        state.setClipboardControl(this.clipboardControlCheckBox.isSelected());
        state.setCopyToDir(this.copyToDirCheckBox.isSelected());
        state.setUploadAndReplace(this.uploadAndReplaceCheckBox.isSelected());
        state.setImageSavePath(this.whereToCopyTextField.getText().trim());
        state.setCloudType(this.defaultCloudComboBox.getSelectedIndex());
    }

    /**
     * 撤回是调用
     */
    @Override
    public void reset() {
        log.trace("reset invoke");
        OssState state = ossPersistenConfig.getState();
        resetAliyunConfigs(state);
        resetWeiboConfigs(state);
        resetGeneralCOnfigs(state);
        resetClipboardConfigs(state);
    }

    private void resetAliyunConfigs(@NotNull OssState state) {
        OssState.AliyunOssState aliyunOssState = state.getAliyunOssState();
        this.aliyunOssBucketNameTextField.setText(aliyunOssState.getBucketName());
        this.aliyunOssAccessKeyTextField.setText(aliyunOssState.getAccessKey());
        String aliyunOssAccessSecreKey = aliyunOssState.getAccessSecretKey();
        this.aliyunOssAccessSecretKeyTextField.setText(DES.decrypt(aliyunOssAccessSecreKey, OssState.ALIYUN));
        this.aliyunOssEndpointTextField.setText(aliyunOssState.getEndpoint());
        this.aliyunOssFileDirTextField.setText(aliyunOssState.getFiledir());
        this.aliyunOssSuffixBoxField.setSelectedItem(aliyunOssState.getFiledir());
    }

    private void resetWeiboConfigs(@NotNull OssState state) {
        OssState.WeiboOssState weiboOssState = state.getWeiboOssState();
        this.weiboUserNameTextField.setText(weiboOssState.getUserName());
        this.weiboPasswordField.setText(DES.decrypt(weiboOssState.getPassword(), OssState.WEIBOKEY));
    }

    private void resetGeneralCOnfigs(OssState state) {
        this.changeToHtmlTagCheckBox.setSelected(state.isChangeToHtmlTag());
        this.largePictureRadioButton.setSelected(state.getTagType().equals(ImageMarkEnum.LARGE_PICTURE.text));
        this.commonRadioButton.setSelected(state.getTagType().equals(ImageMarkEnum.CUSTOM.text));
        this.customRadioButton.setSelected(state.getTagType().equals(ImageMarkEnum.CUSTOM.text));
        this.customHtmlTypeTextField.setText(state.getTagTypeCode());
        this.compressCheckBox.setSelected(state.isCompress());
        this.compressBeforeUploadCheckBox.setSelected(state.isCompressBeforeUpload());
        this.compressAtLookupCheckBox.setSelected(state.isCompressAtLookup());
        this.compressSlider.setValue(state.getCompressBeforeUploadOfPercent());
        this.compressLabel.setText(String.valueOf(compressSlider.getValue()));
        this.styleNameTextField.setText(state.getStyleName());
        this.transportCheckBox.setSelected(state.isTransport());
        this.backupCheckBox.setSelected(state.isBackup());
    }

    private void resetClipboardConfigs(OssState state){
        this.clipboardControlCheckBox.setSelected(state.isClipboardControl());
        this.copyToDirCheckBox.setSelected(state.isCopyToDir());
        this.whereToCopyTextField.setText(state.getImageSavePath());
        this.uploadAndReplaceCheckBox.setSelected(state.isUploadAndReplace());
        this.defaultCloudComboBox.setSelectedIndex(state.getCloudType());
    }

    @NotNull
    @Override
    public String getId() {
        log.trace("getId invoke");
        return getDisplayName();
    }
}
