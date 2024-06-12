#ifndef MAGICBOX_INPUTEVENT_H
#define MAGICBOX_INPUTEVENT_H

class InputEvent {
public:
    int eventType;
    int keycode;
    int modifier;
    float x;
    float y;
    float down_x;
    float down_y;
};

#endif //MAGICBOX_INPUTEVENT_H
