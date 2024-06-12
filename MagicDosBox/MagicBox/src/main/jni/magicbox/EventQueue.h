#ifndef MAGICBOX_EVENTQUEUE_H
#define MAGICBOX_EVENTQUEUE_H

#include "InputEvent.h"

class EventQueue {
public:
    static const int size = 256;
    volatile int readIndex;
    volatile int writeIndex;
    InputEvent *events;

    EventQueue() {
        readIndex = 0;
        writeIndex = 0;
        events = new InputEvent[size];
/*        for (int i=0; i<size;i++) {
            events[i] = new InputEvent();
        }*/
    }

    InputEvent* getReadEvent() {
        if (writeIndex == readIndex) {
            return 0; // queue empty
        }
        return events + readIndex;
    }

/*
 * M-HT
Je tam este jeden problem - po zavolani funkcie Android_PollEvent je este moznost ze vrateny event bude prepisany skor ako bude precitany.
Da sa to opravit zmenou metody getWriteEvent - namiesto riadku:

if (((writeIndex + 1) & (size - 1)) == readIndex) {

treba pouzit riadok:

if (((writeIndex + 2) & (size - 1)) == readIndex) {
*/

    InputEvent* getWriteEvent() {
        //if (((writeIndex + 1) & (size - 1)) == readIndex) {
        if (((writeIndex + 2) & (size - 1)) == readIndex) {
            return 0; // queue full
        }
        return events + writeIndex;
    }

    void incReadIndex() {
        readIndex = (readIndex + 1) & (size - 1);
    }

    void incWriteIndex() {
        writeIndex = (writeIndex + 1) & (size - 1);
    }
};


#endif
