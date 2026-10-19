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
            description = "Showcases SCREEN 1 graphics mode, JOYSTICK overlay, JOY() and FIRE() inputs, 8x8 sprites, and SPRITEHIT collisions.",
            code = """10 REM *** RETRO ARCADE DEMO ***
20 SCREEN 1
25 JOYSTICK 1
30 CLS
40 COLOR 14
50 PRINT "RETRO ARCADE: USE JOYSTICK & FIRE"
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
230 REM Main Game Loop: Read 8-Way Joystick & Fire Button
240 FOR T = 1 TO 60
250   SPRITEMOVE 0, 2, 0
260   DX = JOY(1) * 3 : DY = JOY(2) * 3
265   SPRITEMOVE 1, DX, DY
270   IF FIRE() = 1 THEN SFX 0
280   HIT = SPRITEHIT(0, 1)
290   IF HIT = 1 THEN GOTO 340
300   SLEEP 50
310 NEXT T
320 GOTO 370
330 REM Collision Event
340 SFX 2
350 CIRCLE (140, 140), 20, 2, 1
360 PRINT "ALIEN INTERCEPTED!"
370 COLOR 10
380 PRINT "Arcade Mission Complete!"
390 END
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
240 PRINT "SFX 0: Laser | SFX 1: Jump | SFX 3: Coin"
250 SFX 0 : SLEEP 250 : SFX 1 : SLEEP 250 : SFX 3
260 SLEEP 350
270 PRINT "SFX 9: Warp Portal | SFX 11: Spring Bounce"
280 SFX 9 : SLEEP 400 : SFX 11
290 SLEEP 350
300 PRINT "SFX 7: Triumphant Victory Fanfare!"
310 SFX 7
320 SLEEP 600
330 COLOR 10
340 PRINT ""
350 PRINT "Audio Test Completed Successfully! (13 SFX Presets Available)"
360 END
"""
        ),
        BasicSample(
            title = "Retro Arcade Pong",
            description = "Classic paddle table tennis with ball physics, wall bounces, paddle deflection, score tracking, and SFX.",
            code = """10 REM *** RETRO ARCADE PONG ***
20 SCREEN 1
30 JOYSTICK 1
40 CLS
50 COLOR 14
60 PRINT "RETRO PONG: STEER PADDLE WITH JOYSTICK"
70 REM Initialize Coordinates & Velocity
80 PX = 130
90 PW = 60
100 BX = 160 : BY = 50
110 VX = 3 : VY = 3
120 SCORE = 0 : LIVES = 3
130 REM --- MAIN GAME LOOP ---
140 CLS
150 COLOR 15
160 PRINT "SCORE:"; SCORE; "  LIVES:"; LIVES
170 REM Draw Arena Border
180 RECT (0, 15)-(319, 199), 1, 0
190 REM Draw Player Paddle
200 RECT (PX, 184)-(PX + PW, 190), 11, 1
210 REM Draw Bouncing Ball
220 CIRCLE (BX, BY), 4, 14, 1
230 REM Read Horizontal Joystick
240 DX = JOY(1) * 6
250 PX = PX + DX
260 IF PX < 3 THEN PX = 3
270 IF PX > 255 THEN PX = 255
280 REM Update Ball Position
290 BX = BX + VX
300 BY = BY + VY
310 REM Left & Right Wall Bounces
320 IF BX <= 8 THEN GOSUB 500
330 IF BX >= 311 THEN GOSUB 520
340 REM Top Ceiling Bounce
350 IF BY <= 23 THEN GOSUB 540
360 REM Paddle Collision Check
370 IF BY >= 178 AND BY <= 186 AND BX >= PX AND BX <= PX + PW THEN GOSUB 560
380 REM Missed Ball Pit Check
390 IF BY >= 195 THEN GOTO 600
400 REM Frame Timing
410 SLEEP 35
420 GOTO 140
430 REM --- SUBROUTINES ---
500 VX = ABS(VX) : SFX 8 : RETURN
520 VX = -ABS(VX) : SFX 8 : RETURN
540 VY = ABS(VY) : SFX 8 : RETURN
560 VY = -ABS(VY) : SCORE = SCORE + 10 : SFX 11 : RETURN
600 REM --- BALL LOST EVENT ---
610 SFX 5
620 LIVES = LIVES - 1
630 IF LIVES <= 0 THEN GOTO 700
640 BX = 160 : BY = 50
650 VX = 3 : VY = 3
660 SLEEP 500
670 GOTO 140
700 REM --- GAME OVER ---
710 SFX 6
720 COLOR 12
730 PRINT ""
740 PRINT "GAME OVER! Final Score: "; SCORE
750 END
"""
        ),
        BasicSample(
            title = "Lunar Lander: Moon Mission",
            description = "Physics simulation with lunar gravity, thruster fuel management, FIRE button, and terrain landing.",
            code = """10 REM *** LUNAR LANDER: APOLLO MOON MISSION ***
20 SCREEN 1
30 JOYSTICK 1
40 CLS
50 COLOR 14
60 PRINT "LUNAR LANDER: FIRE=THRUST, JOY=STEER"
70 REM Lunar Lander Sprite 0 (8x8)
80 SPRITEDEF 0, 8, "00111100011111101111111110111101111111110100001010100101"
90 REM Lander Flight Physics State
100 LX = 150 : LY = 30
110 VX = 0 : VY = 0
120 FUEL = 120
130 SPRITE 0, LX, LY, 0, 15
140 REM --- FLIGHT SIMULATION LOOP ---
150 CLS
160 COLOR 15
170 PRINT "ALT:"; 180 - LY; " FUEL:"; FUEL; " V-SPEED:"; INT(VY * 10)
180 REM Draw Lunar Stars & Horizon
190 LINE (0, 175)-(80, 165), 8
200 LINE (80, 165)-(120, 185), 8
210 REM Landing Pad (X: 120 to 200)
220 RECT (120, 184)-(200, 188), 10, 1
230 LINE (200, 185)-(270, 160), 8
240 LINE (270, 160)-(319, 180), 8
250 REM Gravity Acceleration
260 VY = VY + 0.25
270 REM Main Thruster (FIRE button)
280 IF FIRE() = 1 AND FUEL > 0 THEN GOSUB 500
290 REM Side Attitude Thrusters (Joystick Left/Right)
300 AX = JOY(1)
310 IF AX <> 0 AND FUEL > 0 THEN GOSUB 560
320 REM Position Integration
330 LX = LX + VX
340 LY = LY + VY
350 IF LX < 5 THEN LX = 5 : VX = 0
360 IF LX > 305 THEN LX = 305 : VX = 0
370 SPRITEMOVE 0, INT(VX), INT(VY)
380 REM Touchdown Altitude Check
390 IF LY >= 175 THEN GOTO 600
400 SLEEP 40
410 GOTO 150
500 REM --- MAIN THRUSTER FIRING ---
510 VY = VY - 0.65
520 FUEL = FUEL - 1
530 CIRCLE (LX + 4, LY + 9), 2, 14, 1
540 SFX 8
550 RETURN
560 REM --- RCS SIDE THRUST ---
570 VX = VX + AX * 0.3
580 FUEL = FUEL - 1
590 RETURN
600 REM --- TOUCHDOWN EVALUATION ---
610 IF LX >= 115 AND LX <= 195 THEN GOTO 650
620 REM Crashing outside pad
630 SFX 2
640 COLOR 12 : PRINT "CRASHED INTO LUNAR CRATER!" : END
650 REM On the Landing Pad: Check Touchdown Velocity
660 IF VY <= 1.8 AND ABS(VX) <= 1.0 THEN GOTO 700
670 SFX 2
680 COLOR 12 : PRINT "HARD IMPACT! LANDER DESTROYED!" : END
700 REM Successful Touchdown!
710 SFX 7
720 COLOR 10
730 PRINT "TOUCHDOWN CONFIRMED! THE EAGLE HAS LANDED!"
740 END
"""
        ),
        BasicSample(
            title = "Galaxy Invaders: Space Defender",
            description = "Vertical space shooter with player laser cannons, alien mothership, SPRITEHIT, and explosions.",
            code = """10 REM *** GALAXY INVADERS: SPACE DEFENDER ***
20 SCREEN 1
30 JOYSTICK 1
40 CLS
50 COLOR 14
60 PRINT "DEFEND THE GALAXY: JOY=MOVE, FIRE=SHOOT"
70 REM Sprite 0: Player Starfighter (8x8)
80 SPRITEDEF 0, 8, "00011000001111000111111011111111110110110011110001000010"
90 REM Sprite 1: Alien Invader (8x8)
100 SPRITEDEF 1, 8, "00100100010110100111111011011011111111110100001010100101"
110 REM Sprite 2: Laser Torpedo (8x8)
120 SPRITEDEF 2, 8, "00011000000110000001100000011000000110000001100000000000"
130 REM Spawn Sprites
140 PX = 150 : PY = 175
150 AX = 40 : AY = 30 : ADX = 4
160 LX = 0 : LY = 0 : LACTIVE = 0
170 SCORE = 0
180 SPRITE 0, PX, PY, 0, 11
190 SPRITE 1, AX, AY, 1, 14
200 SPRITE 2, 0, 0, 2, 12 : SPRITESHOW 2, 0
210 REM --- MAIN COMBAT LOOP ---
220 REM Steer Player Ship
230 DX = JOY(1) * 5
240 PX = PX + DX
250 IF PX < 10 THEN PX = 10
260 IF PX > 300 THEN PX = 300
270 SPRITEMOVE 0, DX, 0
280 REM Fire Torpedo
290 IF FIRE() = 1 AND LACTIVE = 0 THEN GOSUB 500
300 REM Move Active Laser Torpedo
310 IF LACTIVE = 1 THEN GOSUB 540
320 REM Move Alien Invader
330 AX = AX + ADX
340 IF AX <= 20 OR AX >= 290 THEN GOSUB 580
350 SPRITEMOVE 1, ADX, 0
360 REM Check Collision: Laser hits Alien
370 IF LACTIVE = 1 AND SPRITEHIT(1, 2) = 1 THEN GOTO 700
380 REM Alien reached defense base line
390 IF AY >= 170 THEN GOTO 800
400 SLEEP 35
410 GOTO 220
500 REM --- LAUNCH TORPEDO ---
510 LACTIVE = 1
520 LX = PX : LY = PY - 10
530 SPRITE 2, LX, LY, 2, 12
535 SPRITESHOW 2, 1
538 SFX 0
539 RETURN
540 REM --- ADVANCE TORPEDO ---
550 LY = LY - 10
560 SPRITEMOVE 2, 0, -10
570 IF LY < 10 THEN LACTIVE = 0 : SPRITESHOW 2, 0
575 RETURN
580 REM --- ALIEN REVERSE & DROP ---
590 ADX = -ADX
600 AY = AY + 12
610 SPRITEMOVE 1, 0, 12
620 SFX 8
630 RETURN
700 REM --- ALIEN INTERCEPTED ---
710 SFX 2
720 SCORE = SCORE + 50
730 LACTIVE = 0 : SPRITESHOW 2, 0
740 COLOR 10
750 PRINT "TARGET DESTROYED! +50 PTS"
760 REM Respawn Alien at Top with Faster Speed
770 AX = 40 : AY = 30
780 IF ADX > 0 THEN ADX = ADX + 1 ELSE ADX = ADX - 1
790 SPRITE 1, AX, AY, 1, 14
795 GOTO 400
800 REM --- BASE BREACHED / GAME OVER ---
810 SFX 6
820 COLOR 12
830 PRINT "DEFENSE LINE OVERRUN! GAME OVER."
840 PRINT "Final Score: "; SCORE
850 END
"""
        ),
        BasicSample(
            title = "Dungeon Gold Hunt: Goblin Chase",
            description = "Top-down 2D labyrinth exploration: collect 5 golden chalices with 8-way joystick while evading a hunting goblin.",
            code = """10 REM *** DUNGEON GOLD HUNT: GOBLIN CHASE ***
20 SCREEN 1
30 JOYSTICK 1
40 CLS
50 COLOR 14
60 PRINT "DUNGEON HUNT: EVADE GOBLIN & COLLECT 5 GOLD"
70 REM Sprite 0: Hero Adventurer (8x8)
80 SPRITEDEF 0, 8, "0011110001000010101111011001100111111111001001000100001001100110"
90 REM Sprite 1: Goblin Hunter (8x8)
100 SPRITEDEF 1, 8, "1000000101111110110110111111111101111110001001000110011010011001"
110 REM Hero & Goblin Positions
120 HX = 40 : HY = 40
130 GX = 260 : GY = 150
140 GOLD = 0 : HEALTH = 100
150 SPRITE 0, HX, HY, 0, 11
160 SPRITE 1, GX, GY, 1, 12
170 REM Five Gold Target Coordinates
180 DIM CX(5) : DIM CY(5) : DIM CF(5)
190 CX(1) = 80  : CY(1) = 60  : CF(1) = 1
200 CX(2) = 240 : CY(2) = 50  : CF(2) = 1
210 CX(3) = 160 : CY(3) = 100 : CF(3) = 1
220 CX(4) = 70  : CY(4) = 140 : CF(4) = 1
230 CX(5) = 250 : CY(5) = 140 : CF(5) = 1
240 REM Draw Dungeon Arena Walls
250 CLS
260 RECT (10, 20)-(309, 189), 6, 0
270 RECT (100, 70)-(220, 75), 6, 1
280 RECT (100, 125)-(220, 130), 6, 1
290 REM --- MAIN DUNGEON LOOP ---
300 COLOR 15
310 PRINT "GOLD:"; GOLD; "/5  HP:"; HEALTH
320 REM Draw Uncollected Gold Coins
330 FOR I = 1 TO 5
340   IF CF(I) = 1 THEN CIRCLE (CX(I), CY(I)), 3, 14, 1
350 NEXT I
360 REM Read 8-Way Joystick
370 HDX = JOY(1) * 4
380 HDY = JOY(2) * 4
390 HX = HX + HDX : HY = HY + HDY
400 IF HX < 20 THEN HX = 20
410 IF HX > 295 THEN HX = 295
420 IF HY < 30 THEN HY = 30
430 IF HY > 175 THEN HY = 175
440 SPRITEMOVE 0, HDX, HDY
450 REM Goblin Tracking AI (Moves toward Hero)
460 GDX = 0 : GDY = 0
470 IF GX < HX THEN GDX = 2
480 IF GX > HX THEN GDX = -2
490 IF GY < HY THEN GDY = 2
500 IF GY > HY THEN GDY = -2
510 GX = GX + GDX : GY = GY + GDY
520 SPRITEMOVE 1, GDX, GDY
530 REM Check Gold Collection
540 FOR I = 1 TO 5
550   IF CF(I) = 1 AND ABS(HX - CX(I)) < 8 AND ABS(HY - CY(I)) < 8 THEN GOSUB 700
560 NEXT I
570 REM Goblin Collision Check
580 IF SPRITEHIT(0, 1) = 1 THEN GOSUB 800
590 REM Victory Check
600 IF GOLD >= 5 THEN GOTO 900
610 REM Defeat Check
620 IF HEALTH <= 0 THEN GOTO 950
630 SLEEP 40
640 GOTO 300
700 REM --- GOLD COLLECTED ---
710 CF(I) = 0
720 GOLD = GOLD + 1
730 CIRCLE (CX(I), CY(I)), 5, 0, 1
740 SFX 3
750 RETURN
800 REM --- GOBLIN ATTACK ---
810 HEALTH = HEALTH - 15
820 SFX 5
830 GX = 260 : GY = 150 : SPRITE 1, GX, GY, 1, 12
840 RETURN
900 REM --- VICTORY ---
910 SFX 7
920 COLOR 10
930 PRINT "DUNGEON CONQUERED! ALL GOLD RETRIEVED!"
940 END
950 REM --- DEFEAT ---
960 SFX 6
970 COLOR 12
980 PRINT "HERO OVERCOME BY THE DUNGEON GOBLIN!"
990 END
"""
        )
    )
}
