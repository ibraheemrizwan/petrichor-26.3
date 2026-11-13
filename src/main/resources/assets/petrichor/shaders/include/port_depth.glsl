#ifndef PETRICHOR_PORT_DEPTH
#define PETRICHOR_PORT_DEPTH
float petrichorClipDepth(float depth) {
#ifdef RENDERPEARL_DEPTH_IS_ZERO_TO_ONE
    return depth;
#else
    return depth * 2.0 - 1.0;
#endif
}
#endif
