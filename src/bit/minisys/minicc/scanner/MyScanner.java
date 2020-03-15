package bit.minisys.minicc.scanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.symbol.c;
import bit.minisys.minicc.internal.util.MiniCCUtil;

public class MyScanner implements IMiniCCScanner {

    private int lIndex = 0;
    private int cIndex = 0;
    private ArrayList<String> srcLines;
    private HashSet<String> keywordSet;
    private Map<Character, String> deliMap = new HashMap<>();
    private char[] sepChar = { '\'', '\"', '?', '\\', 'a', 'b', 'f', 'n', 'r', 't', 'v' };
    private char[] delimiter = { '[', ']', '(', ')', '{', '}', ',', ';' };
    private char[] cOperator = { '+', '-', '&', '*', '~', '!', '/', '^', '%', '=', '.', ':', '?', '#', '<', '>', '|',
            '`' };
    private char[] cBinaryOp = { '+', '-', '>', '<', '=', '!', '&', '|', '*', '/', '%', '^', '#', ':' };
    private char[] digitSpe = { 'a', 'b', 'c', 'd', 'e', 'f', 'F', 'L', 'l', 'U', 'u', 'x', 'X', 'A', 'B', 'C', 'D',
            'E', '.' };

    // The initial state
    private int identifierState = 0;
    private int constantState = 0;
    private int operatorState = 0;
    private int charState = 0;
    private int stringState = 0;

    public MyScanner() {
        deliMap.put('[', "l_square");
        deliMap.put(']', "r_square");
        deliMap.put('(', "l_paren");
        deliMap.put(')', "r_paren");
        deliMap.put('{', "l_brace");
        deliMap.put('}', "r_brace");
        deliMap.put(',', "comma");
        deliMap.put(';', "semi");
        this.keywordSet = new HashSet<String>();
        this.keywordSet.add("auto");
        this.keywordSet.add("main");
        this.keywordSet.add("break");
        this.keywordSet.add("case");
        this.keywordSet.add("char");
        this.keywordSet.add("const");
        this.keywordSet.add("continue");
        this.keywordSet.add("default");
        this.keywordSet.add("do");
        this.keywordSet.add("double");
        this.keywordSet.add("else");
        this.keywordSet.add("enum");
        this.keywordSet.add("extern");
        this.keywordSet.add("float");
        this.keywordSet.add("for");
        this.keywordSet.add("goto");
        this.keywordSet.add("if");
        this.keywordSet.add("inline");
        this.keywordSet.add("int");
        this.keywordSet.add("long");
        this.keywordSet.add("register");
        this.keywordSet.add("restrict");
        this.keywordSet.add("return");
        this.keywordSet.add("short");
        this.keywordSet.add("signed");
        this.keywordSet.add("sizeof");
        this.keywordSet.add("static");
        this.keywordSet.add("struct");
        this.keywordSet.add("switch");
        this.keywordSet.add("typedef");
        this.keywordSet.add("union");
        this.keywordSet.add("unsigned");
        this.keywordSet.add("void");
        this.keywordSet.add("volatile");
        this.keywordSet.add("while");
    }

    private boolean isHave(char[] str, char c) {
        boolean flag = false;
        int pos = 0;
        for (int i = 0; i < str.length; i++) {
            if (str[i] == c) {
                flag = true;
                pos = i + 1;
                break;
            }
        }
        return flag;
    }

    private boolean isAlpha(char c) {
        return Character.isAlphabetic(c);
    }

    private boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    private boolean isAlphaOrDigit(char c) {
        return Character.isLetterOrDigit(c);
    }

    private char getNextChar() {
        char c = Character.MAX_VALUE;
        while (true) {
            if (lIndex < this.srcLines.size()) {
                String line = this.srcLines.get(lIndex);
                if (cIndex < line.length()) {
                    c = line.charAt(cIndex);
                    cIndex++;
                    break;
                } else {
                    lIndex++;
                    cIndex = 0;
                }
            } else {
                break;
            }
        }
        if (c == '\u001a') {
            c = Character.MAX_VALUE;
        }
        // System.out.println(this.cIndex);
        return c;
    }

    private String genToken(int num, String lexme, String type) {
        // System.out.println(this.cIndex);
        return genToken(num, lexme, type, this.cIndex - 1, this.lIndex);
    }

    private String genToken2(int num, String lexme, String type) {
        return genToken(num, lexme, type, this.cIndex - 2, this.lIndex);
    }

    private String genToken(int num, String lexme, String type, int cIndex, int lIndex) {
        String strToken = "";
        // System.out.println(lexme);
        // System.out.println(type);
        strToken += "[@" + num + "," + (cIndex - lexme.length() + 1) + ":" + cIndex;
        strToken += "='" + lexme + "',<" + type + ">," + (lIndex + 1) + ":" + (cIndex - lexme.length() + 1) + "]\n";

        return strToken;
    }

    @Override
    public String run(String iFile) throws Exception {

        System.out.println("Scanning...");
        String strTokens = "";
        int iTknNum = 0;
        this.srcLines = MiniCCUtil.readFile(iFile);
        String codeValue = "";
        String codeType = "";
        char c = ' ';
        boolean keep = false;
        boolean end = false;
        // System.out.println("Scanning...");
        while (!end) {
            if (c == Character.MAX_VALUE) {
                cIndex = 5;
                strTokens += genToken(iTknNum, "<EOF>", "EOF");
                end = true;
                break;
            }
            if (!keep)
                c = getNextChar();

            keep = false;
            // Identifier
            if (isAlpha(c) || c == '_') {

                codeValue = "";
                while (isAlphaOrDigit(c) || c == '_') {
                    // System.out.println("这是"+c);
                    codeValue += c;
                    c = getNextChar();
                    if (c == Character.MAX_VALUE) {
                        end = true;
                        break;
                    }
                }

                if (c != Character.MAX_VALUE)
                    keep = true;
                if (this.keywordSet.contains(codeValue)) {
                    strTokens += genToken(iTknNum, codeValue, "'" + codeValue + "'");
                    // System.out.println("这是"+c+(c==Character.MAX_VALUE));
                    System.out.println(strTokens);
                } else {
                    strTokens += genToken(iTknNum, codeValue, "Identifier");
                }
                iTknNum++;
            }
            // Constant!
            else if (isDigit(c)) {
                constantState = 0;
                codeValue = "";
                while (isDigit(c) || isHave(digitSpe, c)) {
                    codeValue += c;
                    // System.out.println(codeValue);
                    if (constantState == 0) {
                        if (c == '0')
                            constantState = 2;
                        else
                            constantState = 1;
                    } else if (constantState == 1) {
                        if (isDigit(c))
                            constantState = 1;
                        else if (c == '.')
                            constantState = 3;
                        else if (c == 'L' || c == 'l')
                            constantState = 13;
                        else if (c == 'U' || c == 'u')
                            constantState = 12;
                        else
                            constantState = -1;
                    } else if (constantState == 2) {
                        if (c == '.')
                            constantState = 3;
                        else if (c == 'x' || c == 'X')
                            constantState = 4;
                        else if (c >= '0' && c < '8')
                            constantState = 5;
                        else if (c == 'u' || c == 'U')
                            constantState = 12;
                        else if (c == 'l' || c == 'L')
                            constantState = 13;
                        else
                            constantState = -1;
                    } else if (constantState == 3) {
                        if (isDigit(c))
                            constantState = 7;
                        else
                            constantState = -1;
                    } else if (constantState == 4) {
                        if (isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))
                            constantState = 6;
                        else
                            constantState = -1;
                    } else if (constantState == 5) {
                        if (c >= '0' && c <= '7')
                            constantState = 5;
                        else if (c == 'U' || c == 'u')
                            constantState = 12;
                        else if (c == 'L' || c == 'l')
                            constantState = 13;
                        else
                            constantState = -1;
                    } else if (constantState == 6) {
                        if (isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))
                            constantState = 6;
                        else if (c == 'U' || c == 'u')
                            constantState = 12;
                        else if (c == 'L' || c == 'l')
                            constantState = 13;
                        else if (c == '.')
                            constantState = 17;
                        else
                            constantState = -1;
                    } else if (constantState == 7) {
                        if (isDigit(c))
                            constantState = 7;
                        else if (c == 'E' || c == 'e')
                            constantState = 8;
                        else if (c == 'F' || c == 'f')
                            constantState = 10;
                        else if (c == 'L' || c == 'l')
                            constantState = 11;
                        else
                            constantState = -1;
                    } else if (constantState == 8) {
                        if (isDigit(c))
                            constantState = 7;
                        else if (c == '+' || c == '-')
                            constantState = 9;
                        else
                            constantState = -1;
                    } else if (constantState == 9) {
                        if (isDigit(c))
                            constantState = 7;
                        else
                            constantState = -1;
                    } else if (constantState == 10) {
                        constantState = -1;
                    } else if (constantState == 11) {
                        constantState = -1;
                    } else if (constantState == 12) {
                        if (c == 'L' || c == 'l')
                            constantState = 14;
                        else
                            constantState = 1;
                    } else if (constantState == 13) {
                        if (c == 'U' || c == 'u')
                            constantState = 14;
                        else if (c == 'L' || c == 'l')
                            constantState = 15;
                        else
                            constantState = -1;
                    } else if (constantState == 14) {
                        if (c == 'L' || c == 'l')
                            constantState = 16;
                        else
                            constantState = -1;
                    } else if (constantState == 15) {
                        if (c == 'U' || c == 'u')
                            constantState = 16;
                        else
                            constantState = -1;
                    } else if (constantState == 16) {
                        constantState = -1;
                    } else if (constantState == 17) {
                        if(isDigit(c)||(c>='a'&&c<='f')||(c>='A'&&c<='F'))
                            constantState =18;
                        else if(c=='F'||c=='f') constantState =10;
                        else if(c=='L'||c=='l') constantState=11;
                        else constantState=-1;
                    } else if (constantState == -1) {
                        continue;
                    }
                    else if(constantState==18)
                    {
                        if(c=='p'||c=='P') constantState=19;
                        if(isDigit(c)||(c>='a'&&c<='f')||(c>='A'&&c<='F')) constantState=18;
                        else constantState=-1;
                    }
                    else if(constantState==19)
                    {
                        if(c=='+'||c=='-') 
                    }
                    c = getNextChar();
                    if (c == Character.MAX_VALUE)
                        end = true;
                }
                if (c != Character.MAX_VALUE)
                    keep = true;
                if (constantState != -1) {
                    strTokens += genToken2(iTknNum, codeValue, "numeric_onstant");
                    iTknNum++;
                } else {
                    System.out.println("[ERROR]Scanner:line " + lIndex + ", column=" + cIndex + ", unreachable state!");
                }
            }
            // Character
            else if (c == '\'') {
                charState = 1;
                codeValue = "";
                while (c != Character.MAX_VALUE) {
                    codeValue += c;
                    if (charState == 1) {
                        if (c == '\\')
                            charState = 2;
                        else if (c == '\'')
                            charState = 3;
                    } else if (charState == 2) {
                        if (isHave(sepChar, c)) {
                            charState = 1;
                        }
                    } else if (charState == 3) {
                        break;
                    }
                    c = getNextChar();
                }
                if (charState == 3) {
                    strTokens += genToken2(iTknNum, codeValue, "char_constant");
                    iTknNum++;
                }
            }
            // String
            else if (c == '\"') {
                stringState = 1;
                codeValue = "";
                while (c != Character.MAX_VALUE) {
                    codeValue += c;
                    if (constantState == 1) {
                        if (c == '\\')
                            charState = 2;
                        else if (c == '\"')
                            charState = 3;
                    } else if (charState == 2) {
                        if (isHave(sepChar, c)) {
                            charState = 1;
                        }
                    } else if (charState == 3) {
                        break;
                    }
                    c = getNextChar();
                }
                if (charState == 3) {
                    strTokens += genToken2(iTknNum, codeValue, "string_literal");
                    iTknNum++;
                }
            }
            // Delimiter
            else if (isHave(delimiter, c)) {
                codeValue = "";
                codeValue += c;
                strTokens += genToken2(iTknNum, codeValue, deliMap.get(c));
            }
            // Operator
            else if (isHave(cOperator, c)) {
                codeValue = "";
                operatorState = 0;
                while (isHave(cOperator, c)) {
                    codeValue += c;
                    if (operatorState == 0) {
                        if (isHave(cBinaryOp, c)) {
                            if (c == '+')
                                operatorState = 3;
                            else if (c == '-')
                                operatorState = 4;
                            else if (c == '<')
                                operatorState = 5;
                            else if (c == '>')
                                operatorState = 6;
                            else if (c == '=')
                                operatorState = 7;
                            else if (c == '!')
                                operatorState = 8;
                            else if (c == '&')
                                operatorState = 9;
                            else if (c == '|')
                                operatorState = 10;
                            else if (c == '*')
                                operatorState = 11;
                            else if (c == '/')
                                operatorState = 12;
                            else if (c == '%')
                                operatorState = 13;
                            else if (c == '^')
                                operatorState = 14;
                            else if (c == ':')
                                operatorState = 15;
                            else if (c == '#')
                                operatorState = 16;
                        } else {
                            operatorState = 1;
                            break;
                        }
                    } else if (operatorState == 3) {
                        if (c == '+' || c == '=') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 4) {
                        if (c == '-' || c == '=') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 5) {
                        if (c == ':' || c == '%' || c == '=') {
                            operatorState = 19;
                            break;
                        } else if (c == '<')
                            operatorState = 18;
                        else
                            operatorState = -1;
                    } else if (operatorState == 6) {
                        if (c == '>')
                            operatorState = 17;
                        else if (c == '=') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 7) {
                        if (c == '=') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 8) {
                        if (c == '=') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 9) {
                        if (c == '=' || c == '&') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 10) {
                        if (c == '=' || c == '|') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 11) {
                        if (c == '=') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 12) {
                        if (c == '=') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 13) {
                        if (c == '=' || c == '>' || c == ':') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 14) {
                        if (c == '=') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 15) {
                        if (c == '>') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 16) {
                        if (c == '#') {
                            operatorState = 19;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 17) {
                        if (c == '=') {
                            operatorState = 19;
                            break;
                        } else if (!isHave(cOperator, c)) {
                            operatorState = 19;
                            keep = true;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 18) {
                        if (c == '=') {
                            operatorState = 19;
                            break;
                        } else if (!isHave(cOperator, c)) {
                            operatorState = 19;
                            keep = true;
                            break;
                        } else
                            operatorState = -1;
                    } else if (operatorState == 19)
                        break;
                    else if (operatorState == -1)
                        break;
                    c = getNextChar();
                }
                if (operatorState == 19) {
                    strTokens += genToken2(iTknNum, codeValue, "Multivariate operator");
                    iTknNum++;
                } else if (operatorState == -1) {
                    System.out.println("[ERROR]Scanner:line " + lIndex + ", column=" + cIndex + ", unreachable state!");
                } else {
                    strTokens += genToken2(iTknNum, codeValue, "Unary operator");
                    iTknNum++;
                }
            }
        }
        String oFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_SCANNER_OUTPUT_EXT;
        MiniCCUtil.createAndWriteFile(oFile, strTokens);
        System.out.println(strTokens);
        return oFile;
    }
}
