package com.example.basic.editor

data class BasicSample(
    val title: String,
    val description: String,
    val code: String
)

object SamplePrograms {

    val SAMPLES = listOf(
        BasicSample(
            title = "Number Guessing Game",
            description = "Interactive classic game featuring PRINT, INPUT, RND, and IF conditionals.",
            code = """10 REM *** NUMBER GUESSING GAME ***
20 CLS
30 COLOR 14
40 PRINT "================================"
50 PRINT "   RETRO NUMBER GUESSING GAME   "
60 PRINT "================================"
70 COLOR 15
80 SECRET = INT(RND(100)) + 1
90 TRIES = 0
100 PRINT "I have chosen a number between 1 and 100."
110 PRINT ""
120 TRIES = TRIES + 1
130 INPUT "Enter your guess: ", GUESS
140 IF GUESS = SECRET THEN GOTO 200
150 IF GUESS < SECRET THEN PRINT "Too low! Try higher."
160 IF GUESS > SECRET THEN PRINT "Too high! Try lower."
170 PRINT ""
180 GOTO 120
200 COLOR 10
210 PRINT ""
220 PRINT "CONGRATULATIONS! You guessed it in "; TRIES; " tries!"
230 BEEP
240 END
"""
        ),
        BasicSample(
            title = "Animated Sine Wave",
            description = "Calculates mathematical sine values and prints a dynamic text curve.",
            code = """10 REM *** MATHEMATICAL SINE PLOTTER ***
20 CLS
30 COLOR 11
40 PRINT "Trigonometric Sine Curve Visualizer"
50 FOR A = 0 TO 30 STEP 0.5
60   VAL = SIN(A) * 15 + 18
70   COLOR 13
80   FOR SP = 1 TO INT(VAL)
90     PRINT " ";
100  NEXT SP
110  COLOR 14
120  PRINT "*"
130  SLEEP 50
140 NEXT A
150 COLOR 10
160 PRINT "Visual plotting complete!"
170 END
"""
        ),
        BasicSample(
            title = "Fibonacci Generator",
            description = "Calculates and stores Fibonacci numbers in an array with loop step debugging.",
            code = """10 REM *** FIBONACCI SEQUENCE WITH ARRAYS ***
20 CLS
30 COLOR 14
40 INPUT "How many terms (max 20)? ", N
50 IF N <= 0 THEN END
60 DIM F(25)
70 F(1) = 1
80 F(2) = 1
90 FOR I = 3 TO N
100  F(I) = F(I - 1) + F(I - 2)
110 NEXT I
120 COLOR 10
130 PRINT ""
140 PRINT "The Fibonacci sequence:"
150 FOR I = 1 TO N
160  PRINT "F("; I; ") = "; F(I)
170 NEXT I
180 END
"""
        ),
        BasicSample(
            title = "Factorial & Subroutines",
            description = "Demonstrates GOSUB subroutines and RETURN stack operations.",
            code = """10 REM *** FACTORIAL USING GOSUB SUBROUTINE ***
20 CLS
30 INPUT "Enter a positive number: ", NUM
40 GOSUB 100
50 COLOR 10
60 PRINT "Result: "; NUM; "! = "; FACT
70 END
100 REM --- SUBROUTINE FACTORIAL ---
110 FACT = 1
120 FOR K = 1 TO NUM
130   FACT = FACT * K
140 NEXT K
150 RETURN
"""
        ),
        BasicSample(
            title = "Prime Number Sieve",
            description = "Finds prime numbers in a range using nested loops and modulo.",
            code = """10 REM *** PRIME NUMBER FINDER ***
20 CLS
30 PRINT "Prime Numbers up to 50:"
40 FOR N = 2 TO 50
50   ISPRIME = 1
60   FOR D = 2 TO N - 1
70     IF (N MOD D) = 0 THEN ISPRIME = 0
80   NEXT D
90   IF ISPRIME = 1 THEN PRINT N; " ";
100 NEXT N
110 PRINT ""
120 COLOR 10
130 PRINT "Done finding primes!"
140 END
"""
        )
    )
}
