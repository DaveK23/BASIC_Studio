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
        ),
        BasicSample(
            title = "Retro Sprite Arcade Game",
            description = "Showcases SCREEN 1 graphics mode, 8x8 sprite definition, movement, SOUND synth, and SPRITEHIT collisions.",
            code = """10 REM *** RETRO ARCADE DEMO ***
20 SCREEN 1
30 CLS
40 COLOR 14
50 PRINT "RETRO 320x200 GRAPHICS & SPRITES"
60 REM Draw starry background & mountain horizon
70 RECT (0, 0)-(319, 199), 0, 1
80 FOR S = 1 TO 35
90   PSET (INT(RND(315)) + 2, INT(RND(140)) + 15), 15
100 NEXT S
110 LINE (0, 160)-(100, 120), 9
120 LINE (100, 120)-(200, 170), 9
130 LINE (200, 170)-(319, 130), 9
140 RECT (0, 170)-(319, 199), 5, 1
150 REM Define Alien Sprite 0 (8x8 pixel mask)
160 SPRITEDEF 0, 8, "00100100010110100111111011011011111111110100001010100101"
170 REM Define Hero Ship Sprite 1
180 SPRITEDEF 1, 8, "00011000001111000111111011111111110110110011110001000010"
190 REM Position Sprites
200 SPRITE 0, 40, 50, 0, 4
210 SPRITE 1, 140, 140, 0, 3
220 SFX 4
230 REM Main Game Loop with Animated Patrol & Collision
240 FOR T = 1 TO 40
250   SPRITEMOVE 0, 4, 1
260   SOUND 440 + T * 15, 30, 0
270   HIT = SPRITEHIT(0, 1)
280   IF HIT = 1 THEN GOTO 320
290   SLEEP 60
300 NEXT T
310 GOTO 350
320 REM Collision Event
330 SFX 2
340 CIRCLE (140, 140), 20, 2, 1
350 COLOR 10
360 PRINT "Game Loop Complete!"
370 END
"""
        ),
        BasicSample(
            title = "8-Bit Chiptune & Sound Synth",
            description = "Tests Square, Triangle, Sawtooth, Sine, Noise sound waves, and built-in SFX.",
            code = """10 REM *** 8-BIT AUDIO SYNTHESIS DEMO ***
20 CLS
30 COLOR 14
40 PRINT "8-BIT SOUND SYNTHESIZER DEMO"
50 PRINT "============================"
60 PRINT "Testing Waveforms:"
70 PRINT "1. Square Wave (Pulse)"
80 SOUND 440, 250, 0
90 SLEEP 100
100 PRINT "2. Triangle Wave (Smooth)"
110 SOUND 554, 250, 1
120 SLEEP 100
130 PRINT "3. Sawtooth Wave (Buzzy)"
140 SOUND 659, 250, 2
150 SLEEP 100
160 PRINT "4. Sine Wave (Pure)"
170 SOUND 880, 250, 3
180 SLEEP 100
190 PRINT "5. Noise Generator (Drum)"
200 SOUND 100, 200, 4
210 SLEEP 200
220 PRINT ""
230 PRINT "Testing Retro SFX Presets:"
240 PRINT "SFX 0: Arcade Laser";
250 SFX 0
260 SLEEP 300
270 PRINT " | SFX 1: Platform Jump";
280 SFX 1
290 SLEEP 350
300 PRINT " | SFX 3: Coin Pickup";
310 SFX 3
320 SLEEP 400
330 COLOR 10
340 PRINT ""
350 PRINT "Audio Test Completed Successfully!"
360 END
"""
        )
    )
}
