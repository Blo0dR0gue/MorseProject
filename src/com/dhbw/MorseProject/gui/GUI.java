package com.dhbw.MorseProject.gui;

import com.dhbw.MorseProject.receive.Decoder;
import com.dhbw.MorseProject.send.Encoder;
import com.dhbw.MorseProject.send.events.IEncoderFinishedListener;
import com.dhbw.MorseProject.send.Melody;
import com.dhbw.MorseProject.translate.Translator;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import static javax.swing.JOptionPane.showMessageDialog;

/**
 * Class responsible for user interactions.
 * [ID: F-GUI-*, NF-GUI-* ]
 * @author Mark Mühlenberg, Kai Grübener supported by Frederik Wolter, Lucas Schaffer
 */
public class GUI {
    private JTabbedPane tabbedPane1;
    private JPanel toSend;
    private JPanel toReceive;
    private JPanel toInfo;
    private javax.swing.JPanel mainpanel;
    private JTextField halloTestTextField;
    private JTextArea receive_text_textArea;
    private JTextArea receive_morse_textArea;
    private JButton startRecordingButton;
    private JSlider receive_sensitivity_slider;
    private JButton beginSendingButton;
    private JTextArea send_text_textArea;
    private JTextArea send_morse_textArea;
    private JSlider frequenz_slider;
    private JComboBox comboBox1;
    private JButton send_translate_button;
    private JButton send_clear_button;
    private JButton receive_clear_button;
    private JSplitPane receiveSplitPane;
    private JSplitPane sendSplitPane;
    private JTable table_alphabet;
    private JTextArea textArea_info;
    private JPanel JPanel_border;
    private JTextField textField_info;
    private boolean showingStartRecording = true;
    private boolean showingBeginSend = true;

    private Thread ui_update_thread;

    private enum textArea_focusState {
        FOCUS_GAINED,
        FOCUS_LOST,
        FOCUS_LOST_NEWEST,
        NONE
    }

    private enum textArea {
        MORSE,
        TEXT
    }

    private Map<textArea, textArea_focusState> textAreaFocusMap= new HashMap<textArea, textArea_focusState>();

    /**
     * Constructor for class GUI
     */
    public GUI(){
        /**
         * Settings for frame and application appearance
         */
        JFrame frame = setupJFrame();


        prepareInfoPanel();

        prepareMelodySelectionComboBox();

        configureListeners();

    }

    private void configureListeners() {
        /**
         * Action-Listener to start the recording of audio when startRecordingButton is pressed
         */
        startRecordingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!showingStartRecording){
                    stopRecording(e);

                } else{
                    startRecording(e);
                }
            }
        });

        /**
         * Action-Listener to start and stop sending the given morse-code via audio when beginSendingButton is pressed
         * @see #startPlaying()
         * @see #stopPlaying()
         */
        beginSendingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!showingBeginSend){
                    stopPlaying();
                } else{
                    startPlaying();
                }
            }
        });

        Encoder.getInstance().addFinishedEventListener(new IEncoderFinishedListener() {
            @Override
            public void run() {
                stopPlayingChangeVariables();
            }
        });

        /**
         * Action-Listeners to clear textAreas after clicking the button
         */
        send_clear_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clear_textAreas(send_text_textArea, send_morse_textArea);
            }
        });
        receive_clear_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clear_textAreas(receive_text_textArea, receive_morse_textArea);
            }
        });

        /**
         * todo MARK HIER INPUT
         */
        send_morse_textArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                textAreaFocusMap.put(textArea.MORSE, textArea_focusState.FOCUS_GAINED);
            }
            @Override
            public void focusLost(FocusEvent e) {
                textAreaFocusLost(textAreaFocusMap, textArea.MORSE, textArea.TEXT);
            }
        });
        send_text_textArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                textAreaFocusMap.put(textArea.TEXT, textArea_focusState.FOCUS_GAINED);
            }
            @Override
            public void focusLost(FocusEvent e) {
                textAreaFocusLost(textAreaFocusMap, textArea.TEXT, textArea.MORSE);
            }
        });

        send_translate_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                translateSendTextAreas(textAreaFocusMap);
            }
        });
    }

    private void prepareMelodySelectionComboBox() {
        /**
         * Settings for the ComboBox to select given melodies or frequencies.
         */
        for (int i = 0; i < Melody.getMelodyList().size(); i++){
            comboBox1.addItem(Melody.getMelodyList().get(i).getName());
        }
        comboBox1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frequenz_slider.setEnabled(comboBox1.getSelectedItem().toString().equals("Fest"));
            }
        });
    }

    private void prepareInfoPanel() {
        /**
         * Filling text and table in Information-Tab with content and setting appearance
         * @see #fillTable()
         */
        String[][] data = fillTable();
        String[] columnNames = {"Schriftzeichen", "Morse-Code"};
        DefaultTableModel tableModel = (DefaultTableModel) table_alphabet.getModel();
        tableModel.addColumn(columnNames[0]);
        tableModel.addColumn(columnNames[1]);
        for (String[] datum : data) {
            tableModel.addRow(new Object[]{datum[0], datum[1]});
        }
        DefaultTableCellRenderer centerRendering = new DefaultTableCellRenderer();
        centerRendering.setHorizontalAlignment(JLabel.CENTER);
        table_alphabet.getColumnModel().getColumn(0).setCellRenderer(centerRendering);
        table_alphabet.getColumnModel().getColumn(1).setCellRenderer(centerRendering);

        String info = getInfoText();

        textArea_info.setText(info);
    }

    private JFrame setupJFrame() {
        JFrame frame = new JFrame("Kommunikation via Morsecode - Technikmuseum Kommunikatioinstechnik München");
        frame.add(mainpanel);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        //frame.setResizable(false);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {  //overwriting the standard behaviour when closing the window
                try{
                    stopPlaying();
                    stopRecording(null);
                } catch (Exception exception){
                    exception.printStackTrace();    //TODO: show error message when stopping failed
                }
                frame.dispose();
                System.exit(0);
            }
        });

        try {
            frame.setIconImage(ImageIO.read(new File("src/com/dhbw/MorseProject/gui/Morse_Symbolbild.png")));
            // Quelle: "https://w7.pngwing.com/pngs/27/465/png-transparent-morse-code-computer-icons-communication-others-text-code-morse-code.png"
        } catch (IOException e) {
            e.printStackTrace();
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());
        int taskBarSize = scnMax.bottom;
        int effectiveMaxHeight = (screenSize.height-taskBarSize);

        setWindowDimensions(frame, screenSize, effectiveMaxHeight);

        adjust_splitpane_sizes(sendSplitPane, send_text_textArea, send_morse_textArea);

        adjust_splitpane_sizes(receiveSplitPane, receive_text_textArea, receive_morse_textArea);

        return frame;
    }

    private void setWindowDimensions(JFrame frame, Dimension screenSize, int effectiveMaxHeight) {
        double scale = 0.5;
        double modifier = (int) (1.0/scale);
        Dimension windowSize = new Dimension((int)(((double) screenSize.width)/modifier), (int)(((double) effectiveMaxHeight)/modifier));
        frame.setMinimumSize(windowSize);
        mainpanel.setMinimumSize(windowSize);

        double multiplier = 1 + 1.0/modifier;
        frame.setLocation(screenSize.width - (int) (frame.getWidth()*multiplier), effectiveMaxHeight - (int) (frame.getHeight()*multiplier) );
    }

    private String getInfoText() {
        return """
                Der Morsecode (auch Morsealphabet oder Morsezeichen genannt) ist ein gebräuchlicher Code zur telegrafischen Übermittlung von Buchstaben, Ziffern und weiterer Zeichen. Er bestimmt das Zeitschema, nach dem ein diskretes Signal ein- und ausgeschaltet wird.
                
                Der Code kann als Tonsignal, als Funksignal, als elektrischer Puls mit einer Morsetaste über eine Telefonleitung, mechanisch oder optisch (etwa mit blinkendem Licht) übertragen werden – oder auch mit jedem sonstigen Medium, mit dem zwei verschiedene Zustände (wie etwa Ton oder kein Ton) eindeutig und in der zeitlichen Länge variierbar dargestellt werden können. Dieses Übertragungsverfahren nennt man Morsetelegrafie.
                
                Das manchmal bei Notfällen beschriebene Morsen durch Klopfen an metallischen Verbindungen erfüllt diese Forderung daher nur bedingt, ist aber mit einiger Übung aufgrund des charakteristischen Rhythmus von Morsezeichen verständlich. Es ist abgeleitet von den „Klopfern“ aus der Anfangszeit der Telegrafentechnik, bestehend aus einem Elektromagneten mit Anker in einem akustischen Hohlspiegel. Beim Einschalten erzeugte er ein lautes und beim Abschalten ein etwas leiseres Klopfgeräusch. So konnte man den Klang der Morsezeichen schon vor der Erfindung des Lautsprechers selbst in größeren Betriebsräumen hörbar machen.
                
                
                Auszug aus de.wikipedia.org/wiki/Morsecode
                """;

        //Quelle: https://de.wikipedia.org/wiki/Morsecode
    }

    private void stopRecording(ActionEvent e) {
        boolean success = Decoder.getInstance().stopRecording();

        if(success){
            startRecordingButton.setText("Start Recording");
            showingStartRecording = !showingStartRecording;
        } else {
            showMessageDialog(null, "Es ist ein Fehler aufgetreten, bitte melden Sie sich beim Personal"+ e, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startRecording(ActionEvent e) {
        clear_textAreas(receive_text_textArea, receive_morse_textArea); //Clearing text areas beforehand

        Runnable ui_update_runnable = () -> {
            do {
                try {
                    synchronized (ui_update_thread){
                        ui_update_thread.wait();
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                receive_morse_textArea.append(Decoder.getInstance().getLastSignal());
                String currentMorseTranslation = Translator.morseToText(receive_morse_textArea.getText());
                receive_text_textArea.setText(currentMorseTranslation);
            } while (!showingStartRecording);
        };

        ui_update_thread = new Thread(ui_update_runnable);

        boolean success = Decoder.getInstance().startRecording(ui_update_thread);
        if (success){
            ui_update_thread.start();
            startRecordingButton.setText("Stop Recording");
            showingStartRecording = !showingStartRecording;
        } else {
            showMessageDialog(null, "Es ist ein Fehler aufgetreten, bitte melden Sie sich beim Personal"+ e, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startPlaying() {
        translateSendTextAreas(textAreaFocusMap);
        String morse = send_morse_textArea.getText();
        Melody sendMelody = null;
        if (comboBox1.getSelectedItem() != "Fest"){
            for (Melody melody : Melody.getMelodyList()){
                if (melody.getName().equals(comboBox1.getSelectedItem().toString())){
                    sendMelody = melody;
                }
            }
        }else{
            sendMelody = new Melody("Fest", new int[] {frequenz_slider.getValue()});
        }
        if (sendMelody != null){
            try {
                Encoder.getInstance().send(morse, sendMelody);
                beginSendingButton.setText("Senden beenden");
                showingBeginSend = !showingBeginSend;
            } catch (InterruptedException e) {
                e.printStackTrace();
                showMessageDialog(null, "Es ist ein Fehler aufgetreten, bitte melden Sie sich beim Personal"+e, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void stopPlaying() {
        try {
            Encoder.getInstance().stopPlaying();
            stopPlayingChangeVariables();
        } catch (InterruptedException e) {
            e.printStackTrace();
            showMessageDialog(null, "Es ist ein Fehler aufgetreten, bitte melden Sie sich beim Personal"+e, "Error", JOptionPane.ERROR_MESSAGE);
        }

    }

    private void stopPlayingChangeVariables() {
        beginSendingButton.setText("Senden beginnen");
        showingBeginSend = !showingBeginSend;
    }

    private void textAreaFocusLost(Map textAreaFocusMap, textArea caller, textArea non_caller) {
        if (!textAreaFocusMap.getOrDefault(caller, textArea_focusState.NONE).equals(textArea_focusState.NONE)){ //can only loose focus if it has gained focus before
            if (textAreaFocusMap.getOrDefault(non_caller, textArea_focusState.NONE).equals(textArea_focusState.FOCUS_LOST_NEWEST)
                    || textAreaFocusMap.getOrDefault(non_caller, textArea_focusState.NONE).equals(textArea_focusState.FOCUS_LOST)){
                textAreaFocusMap.put(non_caller, textArea_focusState.FOCUS_LOST);
                textAreaFocusMap.put(caller, textArea_focusState.FOCUS_LOST_NEWEST);
            } else if (textAreaFocusMap.getOrDefault(non_caller, textArea_focusState.NONE).equals(textArea_focusState.NONE)){
                textAreaFocusMap.put(caller, textArea_focusState.FOCUS_LOST_NEWEST);
            } else{
                textAreaFocusMap.put(caller, textArea_focusState.FOCUS_LOST_NEWEST);
            }
        }
    }
    private void translateSendTextAreas(Map textAreaFocusMap) {
        if (textAreaFocusMap.getOrDefault(textArea.TEXT, textArea_focusState.NONE).equals(textArea_focusState.FOCUS_LOST_NEWEST)
                || textAreaFocusMap.getOrDefault(textArea.TEXT, textArea_focusState.NONE).equals(textArea_focusState.FOCUS_GAINED)){
            translateTextAreaTextToMorse();
        } else if (textAreaFocusMap.getOrDefault(textArea.MORSE, textArea_focusState.NONE).equals(textArea_focusState.FOCUS_LOST_NEWEST)
                || textAreaFocusMap.getOrDefault(textArea.MORSE, textArea_focusState.NONE).equals(textArea_focusState.FOCUS_GAINED)){
            translateMorseTextAreaToText();
        } else{
            if (send_morse_textArea.getText().equals("") && !send_text_textArea.getText().equals("")){
                translateTextAreaTextToMorse();
            } else if (!send_morse_textArea.getText().equals("") && send_text_textArea.getText().equals("")){
                translateMorseTextAreaToText();
            } else {
                System.out.println("No way to decide what to translate to which");
            }
        }
    }
    private void translateMorseTextAreaToText() {
        String textTranslation = Translator.morseToText(send_morse_textArea.getText());
        if (textTranslation == null){
            showMessageDialog(null, "Bitte geben Sie nur Text ein, der übersetzt werden kann (Siehe Informationen)", "Falsche Eingabe", JOptionPane.WARNING_MESSAGE);
            send_text_textArea.setText("");
            System.out.println("morseToText_translationError");
        } else{
            send_text_textArea.setText(textTranslation);
        }
    }
    private void translateTextAreaTextToMorse() {
        String morseTranslation = Translator.textToMorse(send_text_textArea.getText());
        if (morseTranslation == null){
            showMessageDialog(null, "Bitte geben Sie nur Text ein, der übersetzt werden kann (Siehe Informationen)", "Falsche Eingabe", JOptionPane.WARNING_MESSAGE);
            send_morse_textArea.setText("");
            System.out.println("textToMorse_translationError");
        } else{
            send_morse_textArea.setText(morseTranslation);
        }
    }

    private void clear_textAreas(JTextArea... textAreas) {
        for (JTextArea textArea:
             textAreas) {
            textArea.setText("");
        }
    }

    private void adjust_splitpane_sizes(JSplitPane splitPane, JTextArea text_textArea, JTextArea morse_textArea) {
        Dimension textAreas_preferredDimension = new Dimension(splitPane.getWidth() / 2, text_textArea.getPreferredSize().height);
        morse_textArea.setPreferredSize(textAreas_preferredDimension);
        morse_textArea.setMinimumSize(textAreas_preferredDimension);
        text_textArea.setPreferredSize(textAreas_preferredDimension);
        text_textArea.setMinimumSize(textAreas_preferredDimension);
        splitPane.setDividerLocation(splitPane.getWidth()/2);
        splitPane.setResizeWeight(0.5);
    }

    public String[][] fillTable(){
        ArrayList allCharacters = new ArrayList<Character>(Translator.getCharToMorse().keySet());
        String[][] data = new String[42][2];
        int counter = 0;
        for(int i=0; i<allCharacters.size(); i++) {
            data[counter][0] = ""+allCharacters.get(i);
            data[counter][1] = Translator.toMorse((char)allCharacters.get(i));
            counter++;
        }
        return data;
    }

    public int getNoiseThreshold(){
        return receive_sensitivity_slider.getValue();
    }
}