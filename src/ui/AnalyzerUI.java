package ui;

import compiler.Lexer;
import compiler.LexerColor;
import compilerTools.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class AnalyzerUI extends JFrame {
    private String title;
    private Directory directorio;
    private ArrayList<Token> tokens;
    private ArrayList<ErrorLSSL> errors;
    private ArrayList<TextColor> textsColor;
    private Timer timerKeyReleased;
    private ArrayList<Production> identProd;
    private HashMap<String, String> identificadores;
    private boolean codeHasBeenCompiled = false;

    private JButton nuevoButton;
    private JButton abrirButton;
    private JButton guardarButton;
    private JButton guardarComoButton;
    private JButton compilarButton;
    private JButton ejecutarButton;
    private JTextPane textPane1;
    private JTextArea textArea2;
    private JTable table1; //Componente léxico, lexema, línea-columna
    private JPanel jPanelMain;
    private final DefaultTableModel defaultTableModel = new DefaultTableModel(null, new String[]{"Componente léxico", "Lexema", "Línea-Columna"});

    public AnalyzerUI() {
        init();
        nuevoButton.addActionListener(e -> {
            directorio.New();
            clearFields();
        });
        abrirButton.addActionListener(e -> {
            if (directorio.Open()) colorAnalysis();
            clearFields();
        });
        guardarButton.addActionListener(e -> {
            if (directorio.Save()) clearFields();
        });
        guardarComoButton.addActionListener(e -> {
            if (directorio.SaveAs()) clearFields();
        });
        compilarButton.addActionListener(e -> {
            if (getTitle().contains("*") || getTitle().equals(title)) {
                if (directorio.Save()) compile();
            } else {
                compile();
            }
        });
        ejecutarButton.addActionListener(e -> {
            compilarButton.doClick();
            if (codeHasBeenCompiled) {
                if (!errors.isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                            "No se puede ejecutar el código ya que encontró uno o mas errores",
                            "Error en la compilación",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    CodeBlock codeBlock = Functions.splitCodeInCodeBlocks(tokens, "{", "}", ";");
                    System.out.println(codeBlock);
                    ArrayList<String> blockOfCode = codeBlock.getBlocksOfCodeInOrderOfExec();
                    System.out.println(blockOfCode);
                }
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new AnalyzerUI();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void init() {
        title = "Compilador";
        this.setTitle(title);
        directorio = new Directory(this, textPane1, title, ".comp");
        this.addWindowListener(new WindowAdapter() { //Para cerrar la ventana
            @Override
            public void windowClosing(WindowEvent e) {
                directorio.Exit();
                System.exit(0);
            }
        });

        Functions.setLineNumberOnJTextComponent(textPane1); //Permite ver los números de línea
        timerKeyReleased = new Timer((int) (1000 * 0.3), e -> { //Timer para colorear las palabras
            timerKeyReleased.stop();
            colorAnalysis();
        });
        Functions.insertAsteriskInName(this, textPane1, () -> timerKeyReleased.restart()); //Indicador de edición
        tokens = new ArrayList<>();
        errors = new ArrayList<>();
        textsColor = new ArrayList<>();
        identificadores = new HashMap<>();
        Functions.setAutocompleterJTextComponent //Autocompletar ciertas palabras
                (new String[]{"Color", "Miguel", "Número"}, textPane1, () -> timerKeyReleased.restart());

        table1.setModel(defaultTableModel);
        this.setContentPane(jPanelMain);
    }

    public void colorAnalysis() {
        textsColor.clear();
        LexerColor lexerColor;

        try{
            File code = new File("color.encrypter");
            try (FileOutputStream outputStream = new FileOutputStream(code)) {
                byte[] byteText = textPane1.getText().getBytes();
                outputStream.write(byteText);
            }
            BufferedReader entries = new BufferedReader(new InputStreamReader(new FileInputStream(code), StandardCharsets.UTF_8));
            lexerColor = new LexerColor(entries);
            while (true){
                TextColor textColor = lexerColor.yylex();
                if (textColor == null) break;
                textsColor.add(textColor);
            }
        }catch (IOException ex){
            System.out.println(ex.getMessage());
        }
    }

    public void clearFields() {
        Functions.clearDataInTable(table1);
        textPane1.setText("");
        textArea2.setText("");
        tokens.clear();
        errors.clear();
        identProd.clear();
        identificadores.clear();
    }

    public void compile() {
        clearFields();
        lexicalAnalysis();
        fillTableTokens();
        syntacticAnalysis();
        semanticAnalysis();
        printConsole();
        codeHasBeenCompiled = true;
    }

    public void lexicalAnalysis() {
        Lexer lexer;
        try{
            File code = new File("code.encrypter");
            try (FileOutputStream output = new FileOutputStream(code)) {
                byte[] bytesText = textPane1.getText().getBytes();
                output.write(bytesText);
            }
            BufferedReader entries = new BufferedReader(new InputStreamReader(new FileInputStream(code), StandardCharsets.UTF_8));
            lexer = new Lexer(entries);
            while(true){
                Token token = lexer.yylex();
                if (token == null) break;
                tokens.add(token);
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void fillTableTokens() {
        tokens.forEach(token -> {
            Object[] data = new Object[]{token.getLexicalComp(), token.getLexeme(), "[%d,%d]".formatted(token.getLine(), token.getColumn())};
            Functions.addRowDataInTable(table1, data);
        });
    }

    public void syntacticAnalysis() {
        Grammar gramatica = new Grammar(tokens, errors);

        /* Eliminacion de errores */
        gramatica.delete(new String[]{"ERROR", "ERROR_1", "ERROR_2"}, 1);
        gramatica.group("VALOR", "NUMERO | COLOR");

        gramatica.group("VARIABLE", "TIPO_DE_DATO IDENTIFICADOR ASIGNASION VALOR", true, identProd);
        gramatica.group("VARIABLE", "IDENTIFICADOR ASIGNACION VALOR", true,
                2, "Error Sintatico {} Falta el tipo de dato en la variable [#,%]");
        gramatica.finalLineColumn();

        gramatica.group("VARIABLE", "TIPO_DE_DATO ASIGNACION VALOR", true,
                3, "Error Sintatico {} Falta de Identificador en la variable [#,%]");
        gramatica.finalLineColumn();

        gramatica.group("VARIABLE", "TIPO_DE_DATO IDENTIFICADOR VALOR", true,
                4, "Error Sintatico {} Faltaa el operador de asignacion en la variable [#,%]");
        gramatica.finalLineColumn();

        gramatica.group("VARIABLE", "TIPO_DE_DATO IDENTIFICADOR ASIGNACION", true,
                5, "Error Sintatico {} Falta el valor en la variable [#,%]");
        gramatica.initialLineColumn();
        gramatica.finalLineColumn();
    }

    public void semanticAnalysis() {
        HashMap<String, String> identDataType = new HashMap<>();
        identDataType.put("color", "COLOR");
        identDataType.put("numero", "NUMERO");
        for (Production id: identProd){
            if (!identDataType.get(id.lexemeRank(0)).equals(id.lexicalCompRank(-1))) {
                errors.add(new ErrorLSSL(1, "Error Semantico {} : Valor No Compatible con el tipo de dato[#, %]", id, true));
            }else if(id.lexicalCompRank(-1).equals("COLOR") && !id.lexemeRank(-1).matches("[0-9a-fA-F]+")){
                errors.add(new ErrorLSSL(1, "Error Semantico {} : El Color no es un numero Hexadecimal [#, %]", id, false));
            }else{
                identificadores.put(id.lexemeRank(1), id.lexemeRank(-1));
            }
       }
    }

    public void printConsole() {
        int sizeErrors = errors.size();
        if (sizeErrors > 0) {
            Functions.sortErrorsByLineAndColumn(errors);
            String strErrors = "\n";
            for (ErrorLSSL error: errors){
                String strERROR = String.valueOf(error);
                strErrors += strERROR + "\n";
            }
            textArea2.setText("Compilación terminada...\n" + strErrors + "\nLa compilación terminó con errores...");
        } else {
            textArea2.setText("Compilación exitosa.");
        }
        textArea2.setCaretPosition(0);
    }
}
