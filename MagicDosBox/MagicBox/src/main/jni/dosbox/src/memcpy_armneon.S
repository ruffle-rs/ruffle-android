        .fpu neon
        .text

        .global memcpy_armneon
        .func   memcpy_armneon
memcpy_armneon:
        push            {r4-r11}
        mov             r3, r0
1:      subs            r2, r2, #128
        pld             [r1, #64]
        pld             [r1, #256]
        pld             [r1, #320]
        ldm             r1!, {r4-r11}
        vld1.64         {d0-d3},   [r1,:128]!
        vld1.64         {d4-d7},   [r1,:128]!
        vld1.64         {d16-d19}, [r1,:128]!
        stm             r3!, {r4-r11}
        vst1.64         {d0-d3},   [r3,:128]!
        vst1.64         {d4-d7},   [r3,:128]!
        vst1.64         {d16-d19}, [r3,:128]!
        bgt             1b
        pop             {r4-r11}
        bx              lr
        .endfunc
