package client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import service.serializedClasses.*;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class Controller  implements Initializable{
    @FXML
    public TextArea textArea;
    public VBox localPanel;
    public VBox serverPanel;
    public TextField textField;
    public MenuBar menuBar;
    public HBox basicButtonsPanel;
    public HBox basicPanel;
    public HBox filesPanel;
    public TextField loginField;
    public PasswordField passwordField;
    public VBox authPanel;

    public MenuButton menuButton;


    private final String ADDRESS = "localhost";
    private final int PORT = 45081;


    private boolean authenticated = false;
    private String nickname;
    private String login;
    private String password;

    public Stage regStage;

    public RegController getRegController() {
        return regController;
    }

    private RegController regController;
    public static final int MB_20 = 20 * 1_000_000;

    private Channel channel;
    private Bootstrap bootstrap;
    private LocalFilePanelController localPC;
    private ServerFilePanelController serverPC;

    public String getNickname() {
        return nickname;
    }

    public LocalFilePanelController getLocalPC() {
        return localPC;
    }

    private Network network;

    public ServerFilePanelController getServerPC() {
        return serverPC;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        network = new Network(this);
        setAuthenticated(false);
    }

    public void connect(){
        //Вынести в отдельный класс! отправку сообщений тоже
            EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
            try {
                bootstrap = new Bootstrap();
                bootstrap.group(eventLoopGroup);
                bootstrap.channel(NioSocketChannel.class);
                bootstrap.remoteAddress(ADDRESS, PORT);
                Controller controller = this;
                bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        socketChannel.pipeline().addLast(
                                new ObjectDecoder(MB_20, ClassResolvers.cacheDisabled(null)),
                                new ObjectEncoder(),
                                new ServerHandler(controller)
                        );
                    }
                });
                ChannelFuture channelFuture =  bootstrap.connect().sync();
                channel = channelFuture.channel();
                channelFuture.channel().closeFuture();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } //finally {
              //  eventLoopGroup.shutdownGracefully();
           // }

        //сделать отдельный метод close channel.close
    }

    public void clickBtnAuth(ActionEvent actionEvent) {
        network.sendRequest(new AuthRequest(loginField.getText().trim(), passwordField.getText().trim()));
        login = loginField.getText().trim();
        //passwordField.clear();
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        textArea.setVisible(!authenticated);
        textArea.setManaged(!authenticated);
        filesPanel.setVisible(authenticated);
        filesPanel.setManaged(authenticated);
        menuBar.setVisible(authenticated);
        menuBar.setManaged(authenticated);
        basicButtonsPanel.setVisible(authenticated);
        basicButtonsPanel.setManaged(authenticated);

        if (!authenticated) {
            nickname = "";
        }

        textArea.clear();

        if (authenticated){
            login = loginField.getText().trim();
            password = passwordField.getText().trim();
            localPC = (LocalFilePanelController) localPanel.getProperties().get("ctrl");

            serverPC = (ServerFilePanelController) serverPanel.getProperties().get("ctrl");
            serverPC.setController(this);
        }
    }

    public void clickBtnReg(ActionEvent actionEvent){
        if (regStage == null){
            createRegWindow();
        }
        regStage.show();
    }

    private void createRegWindow(){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("GeekStorage - Регистрация нового пользователя");
            regStage.setScene(new Scene(root, 500, 425));

            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.initStyle(StageStyle.UTILITY);

            regController = fxmlLoader.getController();
            regController.setNetwork(network);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public void btnExit(ActionEvent actionEvent) {
        network.close();
        Platform.exit();
    }

    public void copyButtonAction(ActionEvent actionEvent) {
        if (localPC.getSelectedFileName() == null && serverPC.getSelectedFileName() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл для копирования не выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        //Копировование с локала на сервер
        try {
            //Копировование с локала на сервер
            if (localPC.getSelectedFileName() != null) {
                LocalFilePanelController srcPC = null;
                ServerFilePanelController dstPC = null;
                srcPC = localPC;
                dstPC = serverPC;
                Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFileName());
                Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());

                FileInputStream fileInputStream = new FileInputStream(srcPath.toFile());
                byte[] data = new byte[fileInputStream.available()];
                fileInputStream.read(data);
                fileInputStream.close();
                network.sendRequest(new SendFileRequest(login, password, data, dstPath.toString(), srcPath.toString()));
                network.sendRequest(new GetFileListRequest(login, password, dstPC.getCurrentPath()));
            } else if (serverPC.getSelectedFileName() != null) {
                ServerFilePanelController srcPC = null;
                LocalFilePanelController dstPC = null;
                srcPC = serverPC;
                dstPC = localPC;
                System.out.println(srcPC.getCurrentPath());
                System.out.println(srcPC.getSelectedFileName());
                Path srcPath = Paths.get(srcPC.getCurrentPath().equals(File.separator) ? "": srcPC.getCurrentPath(), srcPC.getSelectedFileName());
                Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
                System.out.println(dstPath);
                network.sendRequest(new UploadFileRequest(login, password, srcPath.toString(), dstPath.toString()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
            alert.showAndWait();
        }

    }

    public Network getNetwork() {
        return network;
    }
}
