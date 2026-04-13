package logo.analysis

import java.util.Locale

data class BuiltinCommand(
    val name: String,
    val arity: Int,
    val returnsValue: Boolean,
)

object BuiltinCommands {

    private val commands: Map<String, BuiltinCommand> = buildMap {
        fun cmd(name: String, arity: Int) {
            put(name, BuiltinCommand(name, arity, false))
        }

        fun reporter(name: String, arity: Int) {
            put(name, BuiltinCommand(name, arity, true))
        }

        // Turtle movement
        cmd("FORWARD", 1); cmd("FD", 1)
        cmd("BACK", 1); cmd("BK", 1)
        cmd("RIGHT", 1); cmd("RT", 1)
        cmd("LEFT", 1); cmd("LT", 1)
        cmd("HOME", 0)
        cmd("SETXY", 2)
        cmd("SETX", 1); cmd("SETY", 1)
        cmd("SETHEADING", 1); cmd("SETH", 1)
        cmd("ARC", 2)

        // Pen control
        cmd("PENUP", 0); cmd("PU", 0)
        cmd("PENDOWN", 0); cmd("PD", 0)
        cmd("HIDETURTLE", 0); cmd("HT", 0)
        cmd("SHOWTURTLE", 0); cmd("ST", 0)
        cmd("SETPENCOLOR", 1); cmd("SETPC", 1); cmd("SETCOLOR", 1)
        cmd("SETPENSIZE", 1)
        cmd("CLEARSCREEN", 0); cmd("CS", 0)
        cmd("CLEAN", 0)

        // Turtle reporters
        reporter("XCOR", 0)
        reporter("YCOR", 0)
        reporter("HEADING", 0)
        reporter("POS", 0)
        reporter("TOWARDS", 1)

        // Arithmetic
        reporter("SUM", 2)
        reporter("DIFFERENCE", 2)
        reporter("PRODUCT", 2)
        reporter("QUOTIENT", 2)
        reporter("REMAINDER", 2)
        reporter("MODULO", 2)
        reporter("POWER", 2)
        reporter("SQRT", 1)
        reporter("ABS", 1)
        reporter("INT", 1)
        reporter("ROUND", 1)
        reporter("RANDOM", 1)

        // Trigonometry
        reporter("SIN", 1)
        reporter("COS", 1)
        reporter("TAN", 1)
        reporter("ATAN", 1)

        // Comparison reporters
        reporter("EQUALP", 2); reporter("EQUAL?", 2)
        reporter("LESSP", 2); reporter("LESS?", 2)
        reporter("GREATERP", 2); reporter("GREATER?", 2)
        reporter("NOTEQUALP", 2); reporter("NOTEQUAL?", 2)

        // List operations
        reporter("FIRST", 1)
        reporter("LAST", 1)
        reporter("BUTFIRST", 1); reporter("BF", 1)
        reporter("BUTLAST", 1); reporter("BL", 1)
        reporter("FPUT", 2)
        reporter("LPUT", 2)
        reporter("COUNT", 1)
        reporter("ITEM", 2)
        reporter("LIST", 2)
        reporter("SENTENCE", 2); reporter("SE", 2)
        reporter("WORD", 2)

        // Predicates
        reporter("EMPTYP", 1); reporter("EMPTY?", 1)
        reporter("LISTP", 1); reporter("LIST?", 1)
        reporter("WORDP", 1); reporter("WORD?", 1)
        reporter("NUMBERP", 1); reporter("NUMBER?", 1)
        reporter("MEMBERP", 2); reporter("MEMBER?", 2)

        // I/O
        cmd("PRINT", 1); cmd("PR", 1)
        cmd("SHOW", 1)
        cmd("TYPE", 1)
        reporter("READLIST", 0); reporter("RL", 0)
        reporter("READWORD", 0); reporter("RW", 0)

        // Pen extras
        cmd("SETWIDTH", 1); cmd("SETW", 1)
        cmd("PENERASE", 0); cmd("PE", 0)
        cmd("PENREVERSE", 0); cmd("PX", 0)
        reporter("PENCOLOR", 0); reporter("PC", 0)
        reporter("WIDTH", 0)

        // Graphics extras
        cmd("STAMP", 0)
        cmd("FILL", 0)
        cmd("SETBG", 1)
        reporter("BACKGROUND", 0); reporter("BG", 0)
        cmd("DOT", 0)
        reporter("DISTANCE", 1)

        // Trig extras
        reporter("ARCSIN", 1); reporter("ASIN", 1)
        reporter("ARCCOS", 1); reporter("ACOS", 1)
        reporter("ARCTAN2", 2); reporter("ATAN2", 2)
        reporter("LOG", 1)
        reporter("EXP", 1); reporter("EXPN", 1)

        // String operations
        reporter("UPPERCASE", 1)
        reporter("LOWERCASE", 1)
        reporter("CHAR", 1)
        reporter("UNICODE", 1)

        // Type checking extras
        reporter("ARRAY?", 1); reporter("ARRAYP", 1)
        reporter("NAME?", 1); reporter("NAMEP", 1)
        reporter("DEFINED?", 1); reporter("DEFINEDP", 1)

        // Control flow extras
        cmd("UNTIL", 2)
        cmd("DO.WHILE", 2)
        cmd("DO.UNTIL", 2)
        cmd("RUN", 1)

        // I/O extras
        cmd("ALERT", 1)
        reporter("READ", 0)
        reporter("READCHAR", 0); reporter("RC", 0)
        reporter("READLINE", 0)
        reporter("READPROMPT", 1); reporter("RP", 1)

        // Miscellaneous
        cmd("WAIT", 1)
        reporter("THING", 1)
        cmd("HELP", 1)
        reporter("VERSION", 0); reporter("VER", 0)
        reporter("DATE", 0)
        reporter("TIME", 0)
        cmd("BYE", 0); cmd("QUIT", 0); cmd("EXIT", 0)
    }

    fun lookup(name: String): BuiltinCommand? = commands[name.uppercase(Locale.ROOT)]

    fun all(): Collection<BuiltinCommand> = commands.values
}
