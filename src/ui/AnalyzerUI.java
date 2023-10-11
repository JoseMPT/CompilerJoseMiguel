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
    private ArrayList<Production> identProd = new ArrayList<>();
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

        //Functions.setLineNumberOnJTextComponent(textPane1); //Permite ver los números de línea
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

        //table1.setModel(defaultTableModel);
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
        gramatica.delete(new String[]{"ERROR", "ERROR_NUMERO", "ERROR_IDENTIFICADOR"}, 1);
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

        /*Eliminación de tipos de datos y operador de asignación*/
        gramatica.delete("TIPO_DE_DATO", 7,
                "Error Sintatico {} Falta el tipo de dato en la declaración [#,%]");
        gramatica.delete("ASIGNACION", 8,
                "Error Sintatico {} Falta el operador de asignacion en la declaración [#,%]");

        /*Agrupar identificadores y definición de parámetros*/
        gramatica.group("PARAMETROS", "VALOR (COMA VALOR)+");
        gramatica.group("FUNCION", "PALABRA RESERVADA | PINTAR | DETENET_PINTAR | REPETIR | DETENER_REPETIR | ESTRUCTURA_SI", true);

        gramatica.group("FUNCION_COMP", "FUNCION PARENTESIS_IZQ (VALOR | PARAMETROS)? PARENTESIS_DER", true);
        gramatica.group("FUNCION_COMP", "FUNCION (VALOR | PARAMETROS)? PARENTESIS_DER", true, 9, "Error Sintatico {} Falta el parentesis izquierdo en la función [#,%]");
        gramatica.finalLineColumn();

        gramatica.group("FUNCION_COMP", "FUNCION PARENTESIS_IZQ (VALOR | PARAMETROS)?", true, 10, "Error Sintatico {} Falta el parentesis derecho en la función [#,%]");
        gramatica.finalLineColumn();

        /*Eliminación de funciones no declaradas*/
        gramatica.delete("FUNCION", 11, "Error Sintatico {} Función no declarada correctamente [#,%]");
        gramatica.loopForFunExecUntilChangeNotDetected(() -> {
            gramatica.group("EXP_LOGICA", "(FUNCION_COMP | EXP_LOGICA) (OPERADOR_LOGICO (FUNCION_COMP | EXP_LOGICA))+");
            gramatica.group("EXP_LOGICA","PARENTESIS_IZQ (EXP_LOGICA | FUNCION_COMP) PARENTESIS_DER", true);
        });
        //gramatica.group("EXP_LOGICA", "(FUNCION_COMP | EXP_LOGICA) (OPERADOR_LOGICO(FUNCION_COMP | EXP_LOGICA))+", true);

        /*Eliminación de operadores lógicos*/
        gramatica.delete("OPERADOR_LOGICO", 12, "Error Sintatico {} Falta el operador lógico en la expresión lógica [#,%]");

        /*Agrupar expresiones lógicas*/
        gramatica.group("VALOR", "EXP_LOGICA");
        gramatica.group("PARAMETROS", "VALOR(COMA VALOR)+");

        /*Agrupación de estructuras de control*/
        gramatica.group("ESTRUCTURA_CONTROL", "REPETIR | ESTRUCTURA_SI");
        gramatica.group("EST_CONTROL_COMP", "ESTRUCTURA_CONTROL PARENTESIS_IZQ PARENTESIS_DER", true);
        gramatica.group("EST_CONTROL_COMP", "ESTRUCTURA_CONTROL (VALOR | PARAMETROS)", true);
        gramatica.group("EST_CONTROL_COMP", "ESTRUCTURA_CONTROL PARENTESIS_IZQ | (VALOR | PARAMETROS)? PARENTESIS_DER", true);

        /*Eliminación de estructuras de control no imcompletas*/
        gramatica.delete("ESTRUCTURA_CONTROL", 13, "Error Sintatico {} Estructura de control no declarada correctamente [#,%]");

        /*Eliminación de paréntesis*/
        gramatica.delete(new String[]{"PARENTESIS_IZQ", "PARENTESIS_DER"}, 14, "Error Sintatico {}: El parentesis [] no está declarado correctamente [#,%]");

        /*Modificación de punto y coma*/
        gramatica.group("VARIABLE_PC", "VARIABLE PUNTO_COMA");
        gramatica.group("VARIABLE_PC", "VARIABLE", true, 15, "Error Sintatico {} Falta el punto y coma [] en la declaración [#,%]");

        /*Funciones*/
        gramatica.group("FUNCION_COMP_PC", "FUNCION_COMP PUNTO_Y_COMA");
        gramatica.group("FUNCION_COMP_PC", "FUNCION_COMP", true, 16, "Error Sintatico {} Falta el punto y coma [] en la función [#,%]");

        /*Eliminación del punto y coma*/
        gramatica.delete("PUNTO_Y_COMA", 17, "Error Sintatico {} El punto y coma [] no está al final de la sentencia [#,%]");

        /*Sentencias*/
        gramatica.group("SENTENCIA", "(VARIABLE_PC | FUNCION_COMP_PC)+");
        gramatica.loopForFunExecUntilChangeNotDetected(() -> {
            gramatica.group("EST_CONTROL_COMP_LASLC", "EST_CONTROL_COMP LLAVE_IZQ (SENTENCIA)? LLAVE_DER", true);
            gramatica.group("SENTENCIA", "(SENTENCIA | EST_CONTROL_COMP_LASLC)+");
        });

        /*ESTRUCTURA DE FUNCIÓN INCOMPLETA*/
        gramatica.loopForFunExecUntilChangeNotDetected(() -> {
            gramatica.initialLineColumn();
            gramatica.group("EST_CONTROL_COMP_LASLC", "EST_CONTROL_COMP (SENTENCIA)? LLAVE_DER", true,
                    18, "Error Sintatico {} Falta la llave izquierda en la estructura de control [#,%]");
            gramatica.finalLineColumn();

            gramatica.group("EST_CONTROL_LASLC", "EST_CONTROL_COMP LLAVE_IZQ (SENTENCIA)?", true,
                    19, "Error Sintatico {} Falta la llave derecha en la estructura de control [#,%]");
            gramatica.group("SENTENCIA", "(SENTENCIA | EST_CONTROL_COMP_LASLC)+");
        });

        gramatica.delete(new String[]{"LLAVE_IZQ", "LLAVE_DER"}, 20, "Error Sintatico {} La llave [] no está contenida en una agrupación [#,%]");

        gramatica.show();
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
