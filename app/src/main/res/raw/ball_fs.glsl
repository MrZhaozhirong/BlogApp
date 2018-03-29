precision mediump float;

uniform sampler2D u_TextureUnit;
varying vec2 v_TextureCoordinates;

void main()
{
    //vec4 color = vec4(1.0, 0.2, 0.8, 0);
    //gl_FragColor = color;
    gl_FragColor = texture2D(u_TextureUnit, v_TextureCoordinates);
}