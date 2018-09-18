package project1;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import graphicslib3D.Matrix3D;

import javax.swing.*;
import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static graphicslib3D.GLSLUtils.readShaderSource;

public class Project1 extends JFrame implements GLEventListener
{
	private GLCanvas myCanvas;
	private int renderingProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[2];
	private float cameraX, cameraY, cameraZ;
	private int N = 4; // Recursion level
	private float[] vertexPositions = new float[3 * 2]; // Three points, two coordinates
	
	public Project1()
	{
		setTitle("Project 1 - Koch Snowflake");
		setSize(600, 600);
		// Making sure we get a GL4 context for the canvas
		GLProfile profile = GLProfile.get(GLProfile.GL4);
		GLCapabilities capabilities = new GLCapabilities(profile);
		myCanvas = new GLCanvas(capabilities);
		// end GL4 context
		myCanvas.addGLEventListener(this);
		getContentPane().add(myCanvas);
		this.setVisible(true);
	}
	
	public void display(GLAutoDrawable drawable)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		// Define the triangle
		float[] v1 = new float[2];//Two coordinates
		float[] v2 = new float[2];//Two coordinates
		float[] v3 = new float[2];//Two coordinates
		// The first three vertices define the starting triangle
		// Equilateral triangle centered at the origin
		float sideLength = 2.0f;
		// Top vertex - x and y
		v1[0] = 0;
		v1[1] = sideLength * (float) Math.sqrt(3) / 3;
		// Bottom left
		v2[0] = -0.5f * sideLength;
		v2[1] = -(float) Math.sqrt(3) * sideLength / 6;
		// Bottom right
		v3[0] = 0.5f * sideLength;
		v3[1] = -(float) Math.sqrt(3) * sideLength / 6;
		// Done defining triangle
		
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		
		gl.glUseProgram(renderingProgram);
		
		int mv_loc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
		int proj_loc = gl.glGetUniformLocation(renderingProgram, "proj_matrix");
		
		float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		Matrix3D pMat = orthogonal(-1.5f, 1.5f, 1.5f, -1.5f, 0.1f, 1000.0f);
		
		Matrix3D vMat = new Matrix3D();
		vMat.translate(-cameraX, -cameraY, -cameraZ);
		// Just drawing 2D - not moving the object
		Matrix3D mMat = new Matrix3D();
		mMat.setToIdentity();
		
		Matrix3D mvMat = new Matrix3D();
		mvMat.concatenate(vMat);
		mvMat.concatenate(mMat);
		
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvMat.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_loc, 1, false, pMat.getFloatValues(), 0);
		
		//gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0); // We are only passing two components
		gl.glEnableVertexAttribArray(0);
		
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		
		drawTriangle(v1, v2, v3);
		//processTriangle(v1, v2, v3, N);
	}
	
	public void init(GLAutoDrawable drawable)
	{
		GL4 gl = (GL4) drawable.getGL();
		renderingProgram = createShaderProgram();
		cameraX = 0.0f;
		cameraY = 0.0f;
		cameraZ = 3.0f;
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		
	}
	
	private Matrix3D perspective(float fovy, float aspect, float n, float f)
	{
		float q = 1.0f / ((float) Math.tan(Math.toRadians(0.5f * fovy)));
		float A = q / aspect;
		float B = (n + f) / (n - f);
		float C = (2.0f * n * f) / (n - f);
		Matrix3D r = new Matrix3D();
		r.setElementAt(0, 0, A);
		r.setElementAt(1, 1, q);
		r.setElementAt(2, 2, B);
		r.setElementAt(3, 2, -1.0f);
		r.setElementAt(2, 3, C);
		r.setElementAt(3, 3, 0.0f);
		return r;
	}
	
	private Matrix3D orthogonal(float left, float right, float top, float bottom, float near, float far)
	{
		Matrix3D r = new Matrix3D();
		r.setElementAt(0, 0, 2.0 / (right - left));
		r.setElementAt(1, 1, 2.0 / (top - bottom));
		r.setElementAt(2, 2, 1 / (far - near));
		r.setElementAt(3, 3, 1.0f);
		r.setElementAt(0, 3, -(right + left) / (right - left));
		r.setElementAt(1, 3, -(top + bottom) / (top - bottom));
		r.setElementAt(2, 3, -near / (far - near));
		return r;
	}
	
	public static void main(String[] args)
	{
		new Project1();
	}
	
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{
	}
	
	public void dispose(GLAutoDrawable drawable)
	{
	}
	
	private int createShaderProgram()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		String vshaderSource[] = readShaderSource("shaders/vert.shader");
		String fshaderSource[] = readShaderSource("shaders/frag.shader");
		
		int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
		
		gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0);
		gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, null, 0);
		
		gl.glCompileShader(vShader);
		gl.glCompileShader(fShader);
		
		int vfprogram = gl.glCreateProgram();
		gl.glAttachShader(vfprogram, vShader);
		gl.glAttachShader(vfprogram, fShader);
		gl.glLinkProgram(vfprogram);
		return vfprogram;
	}
	
	// Processing triangles
	private void processTriangle(float[] v1, float[] v2, float[] v3, int n)
	{
		
		if(n > 0) // Recurse
		{
			// Coordinates for middle points
			float[] m1 = new float[2];
			float[] m2 = new float[2];
			float[] m3 = new float[2];
			for(int i = 0; i < 2; i++)
			{
				m1[i] = (v1[i] + v2[i]) / 2;
				m2[i] = (v2[i] + v3[i]) / 2;
				m3[i] = (v1[i] + v3[i]) / 2;
			}
			// Recurse
			processTriangle(m1, v2, m2, n - 1);
			processTriangle(v1, m1, m3, n - 1);
			processTriangle(m3, m2, v3, n - 1);
		}
		else
		{
			drawTriangle(v1, v2, v3); // Draw
		}
		
	}
	
	private void drawTriangle(float[] v1, float[] v2, float[] v3)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		// Store points in backing store
		vertexPositions[0] = v1[0];
		vertexPositions[1] = v1[1];
		vertexPositions[2] = v2[0];
		vertexPositions[3] = v2[1];
		vertexPositions[4] = v3[0];
		vertexPositions[5] = v3[1];
		
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(vertexPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);
		
		// Draw now
		gl.glDrawArrays(GL_TRIANGLES, 0, 3);
		
	}
	
}
