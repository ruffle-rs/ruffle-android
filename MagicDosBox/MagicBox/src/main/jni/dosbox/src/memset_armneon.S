        .fpu neon
        .text

        .global memset_armneon
        .func   memset_armneon
memset_armneon:
        push            {r4-r11}
        mov             r3,  r0
        vdup.8          q0,  r1
        vmov            q1,  q0
        orr             r4,  r1, r1, lsl #8
        orr             r4,  r4, r4, lsl #16
        mov             r5,  r4
        mov             r6,  r4
        mov             r7,  r4
        mov             r8,  r4
        mov             r9,  r4
        mov             r10, r4
        mov             r11, r4
        add             r12, r3,  r2, lsr #2
1:      subs            r2,  r2, #128
        pld             [r3, #64]
        stm             r3!, {r4-r11}
        vst1.64         {d0-d3},   [r12,:128]!
        vst1.64         {d0-d3},   [r12,:128]!
        vst1.64         {d0-d3},   [r12,:128]!
        bgt             1b
        pop             {r4-r11}
        bx              lr
        .endfunc