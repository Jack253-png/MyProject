package org.mcreater.disk;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.GsonBuilder;
import net.querz.mca.Chunk;
import net.querz.mca.MCAFile;
import net.querz.mca.MCAUtil;
import net.querz.nbt.io.NBTInputStream;
import net.querz.nbt.tag.Tag;
import org.mcreater.disk.utils.TagUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

public class Main {
    PathTreeNode root;
    JTree tree;
    JFrame jf;
    Thread loadThread;
    JTextArea area;
    JLabel info;

    JProgressBar bar2;
    Thread dicLoadThread;
    File current;
    public static void main(String[] args) throws Throwable {
        new Main().doMain();
    }
    public static class PathTreeNode extends DefaultMutableTreeNode {
        public final File path;
        public PathTreeNode(String name, File path) {
            super(name);
            this.path = path;
        }
    }
    public void doMain(){
        JFrame.setDefaultLookAndFeelDecorated(false);
        FlatLightLaf.install();

        UIManager.installLookAndFeel("Flat Light 主题", "com.formdev.flatlaf.FlatLightLaf");
        UIManager.installLookAndFeel("Flat IntelliJ 主题", "com.formdev.flatlaf.FlatIntelliJLaf");
        UIManager.installLookAndFeel("Flat Dark 主题", "com.formdev.flatlaf.FlatDarkLaf");
        UIManager.installLookAndFeel("Flat Darcula 主题", "com.formdev.flatlaf.FlatDarculaLaf");
        UIManager.installLookAndFeel("Mac OS 主题", ch.randelshofer.quaqua.QuaquaManager.getLookAndFeelClassName());

        JMenuBar bar = new JMenuBar();
        bar.add(createFileMenu());
        bar.add(createOpenMenu());
        bar.add(createAboutMenu());
        bar.add(createThemeMenu());

        jf = new JFrame("文件打开器");
        jf.setSize(600, 400);
        jf.setJMenuBar(bar);

        info = new JLabel();
        bar2 = new JProgressBar(0, 100);

        String path = "D:/mods/util/.minecraft/versions/1.12.2/saves";

        root = new PathTreeNode(path, new File(path));

        area = new JTextArea();
        area.setEditable(false);

        tree = new JTree(new DefaultTreeModel(null));
        tree.addTreeSelectionListener(e -> {
            area.setText("");
            PathTreeNode dmt = (PathTreeNode) tree.getLastSelectedPathComponent();
            if (loadThread != null) loadThread.stop();
            loadThread = new Thread(() -> {
                if (dmt.path != null && dmt.path.isFile()) {
                    area.setText("");
                    jf.setTitle("文件打开器 - " + dmt.path.getName());
                    current = dmt.path;

                    try {
                        NBTInputStream stream = new NBTInputStream(new GZIPInputStream(Files.newInputStream(Paths.get(dmt.path.toURI()))));
                        Tag<?> tag = stream.readTag(Integer.MAX_VALUE).getTag();
                        area.append(new GsonBuilder().setPrettyPrinting().create().toJson(TagUtils.toNativeType(tag)));
                        return;
                    }
                    catch (Exception exc){}

                    try {
                        MCAFile file = MCAUtil.read(dmt.path);
                        Chunk chunk = file.getChunk(0, 0);
                        area.setText(new GsonBuilder().setPrettyPrinting().create().toJson(TagUtils.toNativeType(chunk.data)));
                        return;
                    }
                    catch (Exception exce) {}

                    boolean sh = false;
                    try {
                        InputStream stream = Files.newInputStream(dmt.path.toPath());
                        int available = stream.available();
                        int t0 = available;
                        area.append("                            00     01     02     03     04     05     06     07     08     09     0A     0B     0C     0D     0E     0F\n");

                        byte[] b = new byte[16];

                        int index = 0;
                        bar2.setMaximum(available);
                        while (available != 0) {
                            int temp = available;
                            int len = stream.read(b);
                            if (len == -1) break;
                            available = stream.available();
                            bar2.setValue(index * 16);

                            if (temp < 16) {
                                for (int f = temp; f < 16; f++) {
                                    b[f] = " ".getBytes(StandardCharsets.UTF_8)[0];
                                }
                            }

                            String t = String.valueOf(index * 16);
                            int y = t.length();
                            for (int d = y; d < 26 - y; d++) t += " ";
                            t = "+" + t;

                            area.append(String.format("%s%s     %s     %s     %s     %s     %s     %s     %s     %s     %s     %s     %s     %s     %s     %s     %s",
                                    t,
                                    toHEX(b[0]),
                                    toHEX(b[1]),
                                    toHEX(b[2]),
                                    toHEX(b[3]),
                                    toHEX(b[4]),
                                    toHEX(b[5]),
                                    toHEX(b[6]),
                                    toHEX(b[7]),
                                    toHEX(b[8]),
                                    toHEX(b[9]),
                                    toHEX(b[10]),
                                    toHEX(b[11]),
                                    toHEX(b[12]),
                                    toHEX(b[13]),
                                    toHEX(b[14]),
                                    toHEX(b[15])
                            ));
                            area.append("          " + new String(b).replace("\n", "").replace(" ", ".") + "\n");
                            index++;
                            Thread.sleep(5);
                        }
                        stream.close();
                        bar2.setValue(t0);

                    } catch (Exception ex) {
                        bar2.setString("");
                        bar2.setValue(0);
                        JOptionPane.showMessageDialog(null,"无法读取文件","文件损坏", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            loadThread.start();
        });

        JScrollPane i1 = new JScrollPane(tree);
        JScrollPane i2 = new JScrollPane(area);

        new Thread(() -> {
            while (true) {
                int width = jf.getWidth();
                int height = jf.getHeight();

                i1.setBounds(0, 0, width / 3, height - 56 - 40);
                i2.setBounds(width / 3, 0, width / 3 * 2 - 15, height - 56 - 40);
                info.setBounds(0, height - 56 - 40, width / 3, 20);
                bar2.setBounds(width / 3, height - 90, width / 3 * 2 - 35, 20);
            }
        }).start();

        jf.add(i1);
        jf.add(i2);
        jf.add(info);
        jf.add(bar2);
        jf.resize(jf.getWidth(), jf.getHeight());

        GridLayout layout = new GridLayout();
        layout.setRows(1);
        layout.setColumns(3);

        jf.setLayout(null);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setLocationRelativeTo(null);
        jf.setVisible(true);
        tree.setExpandsSelectedPaths(true);
    }
    private String toHEX(byte b) {
        if (b < 0) return toHEX((byte) (b + 127));
        String s = Integer.toHexString(b);
        if (s.length() == 1) {
            s = "0" + s;
        }
        return s;
    }
    public void addItems(PathTreeNode root, File path) {
        if (path.listFiles() != null) {
            CountDownLatch latch = new CountDownLatch(path.listFiles().length);
            AtomicLong lon = new AtomicLong(path.listFiles().length);
            for (File item : path.listFiles()) {
                PathTreeNode item2 = new PathTreeNode(item.getName(), item);
                info.setText("   " + item.getName());
                root.add(item2);
                if (item.isDirectory()) {
                    if (item.listFiles() != null){
                        addItems(item2, item);
                        if (item.listFiles().length == 0) {
                            item2.add(new PathTreeNode("<empty dictionary>", new File("")));
                        }
                    }
                }
            }
        }
    }
    private JMenu createAboutMenu() {
        JMenu menu = new JMenu("关于(A)");
        menu.setMnemonic(KeyEvent.VK_A);
        JMenuItem item = new JMenuItem("");
        item.setAction(new AbstractAction("Github 仓库(G)") {
            public void actionPerformed(ActionEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(URI.create("https://github.com/Jack253-png/MyProject"));
                    } catch (IOException ignored) {

                    }
                }
            }
        });
        menu.add(item);
        item = new JMenuItem("");
        item.setAction(new AbstractAction("问题反馈(I)") {
            public void actionPerformed(ActionEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(URI.create("https://github.com/Jack253-png/MyProject/issues"));
                    } catch (IOException ignored) {

                    }
                }
            }
        });
        menu.add(item);
        item = new JMenuItem("");
        item.setAction(new AbstractAction("关于作者(U)") {
            public void actionPerformed(ActionEvent e) {
                JPanel panel = new JPanel();
                panel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
                panel.add(new JLabel("作者: "));
                panel.add(new JLabel("      Jack253-png(Github)"));

                JDialog dialog = new JDialog();
                dialog.setContentPane(panel);
                dialog.setTitle("关于作者");
                dialog.setSize(200, 100);
                dialog.setResizable(false);
                dialog.show();
            }
        });
        menu.add(item);

        return menu;
    }
    private JMenu createOpenMenu() {
        JMenu menu=new JMenu("运行(R)");
        menu.setMnemonic(KeyEvent.VK_R);    //设置快速访问符
        JMenuItem item = new JMenuItem("");
        item.setAction(new AbstractAction("用关联的程序打开(T)") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (current != null) {
                        try {
                            desktop.open(current);
                        } catch (IOException ex) {

                        }
                    }
                }
            }
        });
        menu.add(item);

        return menu;
    }
    private JMenu createThemeMenu() {
        JMenu menu=new JMenu("主题(T)");
        menu.setMnemonic(KeyEvent.VK_T);    //设置快速访问符
        JMenuItem item;
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            item = new JMenuItem("");
            item.setAction(new AbstractAction(info.getName()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setLookAndFeel(info.getClassName());
                }
            });
            menu.add(item);
        }

        return menu;
    }

    private void setLookAndFeel(String feel){
        try {
            UIManager.setLookAndFeel(feel);
            SwingUtilities.updateComponentTreeUI(jf);
            JFrame.setDefaultLookAndFeelDecorated(false);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    private JMenu createFileMenu() {
        JMenu menu=new JMenu("文件(F)");
        menu.setMnemonic(KeyEvent.VK_F);    //设置快速访问符
        JMenuItem item=new JMenuItem("",KeyEvent.VK_O);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        item.setAction(new AbstractAction("打开(O)") {
            public void actionPerformed(ActionEvent e) {
                if (loadThread != null) loadThread.stop();
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

                int result = chooser.showOpenDialog(jf);
                if (result == JFileChooser.APPROVE_OPTION) {
                    area.setText("");
                    String path = chooser.getSelectedFile().getAbsolutePath();
                    root = new PathTreeNode(path, new File(path));

                    if (dicLoadThread != null) dicLoadThread.stop();
                    dicLoadThread = new Thread(() -> {
                        tree.setEnabled(false);
                        addItems(root, new File(path));
                        tree.setModel(new DefaultTreeModel(root, false));
                        tree.setEnabled(true);
                        info.setText("");
                    });
                    dicLoadThread.start();
                }
            }
        });
        menu.add(item);
        menu.addSeparator();
        item=new JMenuItem("",KeyEvent.VK_E);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
        item.setAction(new AbstractAction("退出(E)") {
            public void actionPerformed(ActionEvent e) {
                jf.hide();
                System.exit(0);
            }
        });
        menu.add(item);
        return menu;
    }
}