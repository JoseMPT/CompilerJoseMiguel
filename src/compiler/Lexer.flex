package analizer;
import compilerTools.Token;

%%
%class Lexer
%public
%type Token
%line
%column
%{
    private Token token(String lexeme, String lexicalComp, int line, int column) {
        return new Token(lexeme, lexicalComp, line+1, column+1);
    }
%}

/* Variables básicas de comentarios y espacios */
TerminadorDeLinea = \r|\n|\r\n;
EntradaDeCaracter = [^\r\n];
EspacioEnBlanco = {TerminadorDeLinea} | [\t\f ];
ComentarioTradicional = "/*" [^*]* "*"+ ([^/] [^*]* "*"+)* "/";
FinDeLineaComentario = "//" {EntradaDeCaracter}* {TerminadorDeLinea};
ContenidoComentario = ( [^*] | \*+ [^/*] )*;
ComentarioDeDocumentacion = "/**" {ContenidoComentario} "*"+ "/";

/* Comentario */
Comentario = {ComentarioTradicional} | {FinDeLineaComentario} | {ComentarioDeDocumentacion};

/* Identificadores */
Letra = [A-Za-z_ñÑáéíóúÁÉÍÓÚüÜ]
Digito = [0-9]
Identificador = {Letra} ({Letra} | {Digito})*;

/* Número */
Numero = 0 | [1-9] [0-9]*;

%%

/* Comentarios y espacios en blanco */
{EspacioEnBlanco} | {Comentario} { /* Ignorar */ }

/* Identificadores */
\$ {Identificador} { return token(yytext(), "IDENTIFICADOR", yyline, yycolumn); }

/* Tipos de datos */
numero |
color { return token(yytext(), "TIPO_DE_DATO", yyline, yycolumn); }

/* Números */
{Numero} { return token(yytext(), "NUMERO", yyline, yycolumn); }

/* Colores */
# [{Letra}|{Digito}]{6} { return token(yytext(), "COLOR", yyline, yycolumn); }

/* Operadores de agrupación */
"(" { return token(yytext(), "PARENTESIS_IZQ", yyline, yycolumn); }
")" { return token(yytext(), "PARENTESIS_DER", yyline, yycolumn); }

"{" { return token(yytext(), "LLAVE_IZQ", yyline, yycolumn); }
"}" { return token(yytext(), "LLAVE_DER", yyline, yycolumn); }

"[" { return token(yytext(), "CORCHETE_IZQ", yyline, yycolumn); }
"]" { return token(yytext(), "CORCHETE_DER", yyline, yycolumn); }

/* Signos de puntuación */
"," { return token(yytext(), "COMA", yyline, yycolumn); }
";" { return token(yytext(), "PUNTO_Y_COMA", yyline, yycolumn); }

/* Operador de asignación */
--> |
= { return token(yytext(), "ASIGNACION", yyline, yycolumn); }

/* Movimiento */
adelante |
atras |
izquierda |
derecha |
norte |
sur |
este |
oeste { return token(yytext(), "MOVIMIENTO", yyline, yycolumn); }

/* Palabras reservadas */
While | Do | For | Else | If | Int | Float | Char | String | Bool | True | False | Return | Break | Continue | Func | Main | Print | Scan { return token(yytext(), "PALABRA_RESERVADA", yyline, yycolumn); }

/* Pintar */
pintar { return token(yytext(), "PINTAR", yyline, yycolumn); }

/* Detener pintar */
detenerPintar { return token(yytext(), "DETENER_PINTAR", yyline, yycolumn); }

/* Tomar */
tomar |
poner { return token(yytext(), "TOMAR", yyline, yycolumn); }

/* Lanzar moneda */
lanzarMoneda { return token(yytext(), "LANZAR_MONEDA", yyline, yycolumn); }

/* Repetir */
repetir |
repetirMientras { return token(yytext(), "REPETIR", yyline, yycolumn); }

/* Detener repetir */
interrumpir { return token(yytext(), "DETENER_REPETIR", yyline, yycolumn); }

/* Estructura SI */
si |
sino { return token(yytext(), "ESTRUCTURA_SI", yyline, yycolumn); }

/* Operadores lógicos */
"&&" { return token(yytext(), "AND", yyline, yycolumn); }
"||" { return token(yytext(), "OR", yyline, yycolumn); }
"!" { return token(yytext(), "NOT", yyline, yycolumn); }
"&" | "|" { return token(yytext(), "OPERADOR_LOGICO", yyline, yycolumn); }

/* Operadores relacionales */
"==" { return token(yytext(), "IGUAL_QUE", yyline, yycolumn); }
"!=" { return token(yytext(), "DIFERENTE_QUE", yyline, yycolumn); }
"<" { return token(yytext(), "MENOR_QUE", yyline, yycolumn); }
"<=" { return token(yytext(), "MENOR_IGUAL_QUE", yyline, yycolumn); }
">" { return token(yytext(), "MAYOR_QUE", yyline, yycolumn); }
">=" { return token(yytext(), "MAYOR_IGUAL_QUE", yyline, yycolumn); }

/* Operadores aritméticos */
\+ { return token(yytext(), "SUMA", yyline, yycolumn); }
\- { return token(yytext(), "RESTA", yyline, yycolumn); }
\* { return token(yytext(), "MULTIPLICACION", yyline, yycolumn); }
\/ { return token(yytext(), "DIVISION", yyline, yycolumn); }

/* Operadores de incremento y decremento */
\+\+ { return token(yytext(), "INCREMENTO", yyline, yycolumn); }
\-\- { return token(yytext(), "DECREMENTO", yyline, yycolumn); }

/* Final */
final { return token(yytext(), "FINAL", yyline, yycolumn); }

/* Número erróneo */
0{Numero} { return token(yytext(), "ERROR_NUMERO", yyline, yycolumn); }

/* Identificador erróneo */
{Identificador} { return token(yytext(), "ERROR_IDENTIFICADOR", yyline, yycolumn); }

. { return token(yytext(), "ERROR", yyline, yycolumn); }