package analizer;
import compilerTools.TextColor;
import java.awt.Color;

%%
%class LexerColor
%public
%type TextColor
%char
%{
    private TextColor textColor(long start, int size, Color color) {
        return new TextColor((int) start, size, color);
    }
%}

/* Variables básicas de comentarios y espacios */
TerminadorDeLinea = [\r|\n|r\n]
EntradaDeCaracter = [^\r\n]
EspacioEnBlanco = {TerminadorDeLinea} | [ \t\f]
ComentarioTradicional = "/*" [^*] ~"*/" | "/*" "*"+ "/"
FinDeLineaComentario = "//" {EntradaDeCaracter}* {TerminadorDeLinea}?
ContenidoComentario = ( [^*] | \*+ [^/*] )*
ComentarioDeDocumentacion = "/**" {ContenidoComentario} "*"+ "/"

/* Comentario */
Comentario = {ComentarioTradicional}* ( {FinDeLineaComentario} | {ComentarioDeDocumentacion} )

/* Identificador */
Letra = [A-Za-z_ñÑáéíóúÁÉÍÓÚüÜ]
Digito = [0-9]
Identificador = ({Letra}) ({Letra} | {Digito})*

/* Numero */
Numero = 0 | [1-9][0-9]*
%%

/* Comentario o espacios en blanco */
{Comentario} { return textColor(yychar, yylength(), new Color(146, 146, 146)); }
{EspacioEnBlanco} { /*Ignorar*/ }

/* Identificador */
\${Identificador} { /*Ignorar*/ }

/* Tipo de dato */
numero |
color { return textColor(yychar, yylength(), Color.red); }

/* Colores */
#[{Letra}|{Digito}]{6} { return textColor(yychar, yylength(), new Color(0, 255, 127)); }

/* Números */
{Numero} { return textColor(yychar, yylength(), new Color(251, 140, 0)); }

/* Operadores de agrupación */
"(" | ")" { return textColor(yychar, yylength(), new Color(251, 140, 0)); }

/* Signos de puntuación */
"," | ";" { return textColor(yychar, yylength(), new Color(0, 0, 0)); }

/* Operadores de asignación */
--> { return textColor(yychar, yylength(), new Color(255, 215, 0)); }

/* Palabras reservadas */
While |
Do |
For |
If |
Else { return textColor(yychar, yylength(), new Color(128, 0, 0)); }

/* Pintar */
pintar { return textColor(yychar, yylength(), new Color(255, 251, 0)); }

/* Detener pintar */
detenerPintar { return textColor(yychar, yylength(), new Color(255, 64, 129)); }

/* Repetir */
repetir |
repetirMientras { return textColor(yychar, yylength(), new Color(121, 107, 255)); }

/* Detener repetir */
interrumpir { return textColor(yychar, yylength(), new Color(255, 64, 129)); }

/* Estructuras SI */
si |
sino { return textColor(yychar, yylength(), new Color(48, 63, 129)); }

/* Operadores lógicos */
"&" |
"|" { return textColor(yychar, yylength(), new Color(112, 128, 144)); }

/* Final */
final { return textColor(yychar, yylength(), new Color(198, 40, 40)); }

/* Número erróneo */
0{Numero} { /*Ignorar*/ }

/* Identificador */
{Identificador} { /*Ignorar*/ }

. { /*Ignorar*/ }

