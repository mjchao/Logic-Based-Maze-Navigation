package mjchao.mazenav.logic;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import mjchao.mazenav.logic.structures.GeometryWorld;
import mjchao.mazenav.logic.structures.IntegerWorld;
import mjchao.mazenav.logic.structures.NumbersFOL;
import mjchao.mazenav.logic.structures.Operator;
import mjchao.mazenav.logic.structures.Quantifier;
import mjchao.mazenav.logic.structures.Symbol;
import mjchao.mazenav.logic.structures.SymbolTracker;

public class ProcessorTokenizeTest {

	/**
	 * Accessor to the private tokens field of the Processor
	 * class.
	 * 
	 * @param p
	 * @return
	 */
	public ArrayList< Symbol > getTokens( Processor p ) {
		Class<?> c = Processor.class;
		try {
			Field f = c.getDeclaredField( "tokens" );
			f.setAccessible( true );
			return (ArrayList<Symbol>) f.get( p );
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
			throw new RuntimeException( "Could not apply getTokens() method to Processor object." );
		}
	}
	
	@Test
	public void tokenizeBySymbols() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Method preprocess = Processor.class.getDeclaredMethod( "tokenizeByReservedSymbols" , String.class );
		preprocess.setAccessible( true );
		
		//distinguishing between ! and !=
		String input = "(!x OR y) && (x != y)";
		String[] expected = new String[] { "(" , "!" , "x" , "OR" , "y" , ")" , "&&" , "(" , "x" , "!=" , "y" , ")" };
		String[] found = (String[])preprocess.invoke( null , input );
		Assert.assertArrayEquals( expected , found );
		
		//distinguishing between EQUALS and NEQUALS
		input = "FORALL(x, y), !(x EQUALS y) <=> x NEQUALS y";
		expected = new String[] { "FORALL" , "(" , "x" , "," , "y" , ")" , "," , 
				"!" , "(" , "x" , "EQUALS" , "y" , ")" , "<=>" , "x" , 
				"NEQUALS" , "y" };
		found = (String[])preprocess.invoke( null , input );
		Assert.assertArrayEquals( expected , found );
		
		//distinguishing between => and <=> and lack of spaces
		input = "x==y||x=>y||x<=>y";
		expected = new String[] { "x" , "==" , "y" , "||" , "x" , "=>" , "y" , "||" , "x" , "<=>" , "y" };
		found = (String[]) preprocess.invoke( null , input );
		Assert.assertArrayEquals( expected , found );
		
		//dealing with too many spaces
		input = "FORALL     x        !       x     <=>      EXISTS y";
		expected = new String[] { "FORALL" , "x" , "!" , "x" , "<=>" , "EXISTS" , "y" };
		found = (String[]) preprocess.invoke( null , input );
		Assert.assertArrayEquals( expected , found );	
	}
		
	@Test
	public void tokenizeBySymbolsReservedKeywordsInVariableNames() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method preprocess = Processor.class.getDeclaredMethod( "tokenizeByReservedSymbols" , String.class );
		preprocess.setAccessible( true );
		
		String input;
		String[] expected;
		String[] found;
		
		//realizing that OR might be part of a variable name
		input = "!xORy ORy yOR";
		expected = new String[] { "!" , "xORy" , "ORy" , "yOR" };
		found = (String[]) preprocess.invoke( null , input );
		Assert.assertArrayEquals( expected , found );
		
		//edge cases with reserved keywords in variable names
		input = "OR";
		expected = new String[] {"OR"};
		found = (String[]) preprocess.invoke( null , input );
		Assert.assertArrayEquals( expected , found );
		
		input = "ORx";
		expected = new String[] {"ORx"};
		found = (String[]) preprocess.invoke( null , input );
		Assert.assertArrayEquals( expected , found );
	}
	
	@Test
	public void tokenizeBySymbolsEdgeCases() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method preprocess = Processor.class.getDeclaredMethod( "tokenizeByReservedSymbols" , String.class );
		preprocess.setAccessible( true );
		
		String input;
		String[] expected;
		String[] found;
		
		//other edge cases
		input = "or";
		expected = new String[] {"or"};
		found = (String[]) preprocess.invoke( null , input );
		Assert.assertArrayEquals( expected , found );
		
		input = "";
		expected = new String[] {};
		found = (String[]) preprocess.invoke( null , input );
		Assert.assertArrayEquals( expected , found );
		
		input = "          ";
		expected = new String[] {};
		found = (String[]) preprocess.invoke( null , input );
		Assert.assertArrayEquals( expected , found );
		
		input = ">";
		expected = new String[] { ">" };
		found = (String[]) preprocess.invoke( null , input );
		Assert.assertArrayEquals( expected , found );
	}
	
	@Test
	public void testTokenizeWithoutStructures() {
		//test without having to preload a SymbolTracker from
		//some file
		
		Processor test;
		String logicStatement;
		SymbolTracker tracker = new SymbolTracker();
		List<Symbol> tokens;
		List<Symbol> expected;
		
		//basic acceptance test:
		logicStatement = "FORALL x, x==1";
		tracker = new SymbolTracker();
		test = new Processor( logicStatement , tracker );
		test.tokenize();
		tokens = getTokens( test );
		expected = Arrays.asList( Quantifier.FORALL , tracker.getVariableByName( "x" ) , 
				Symbol.COMMA , tracker.getVariableByName( "x" ) , 
				Operator.EQUALS , NumbersFOL.fromInt( 1 ) );
		Assert.assertTrue( tokens.equals( expected ) );
		
		//longer basic acceptance test:
		logicStatement = "FORALL(x, y), EXISTS z S.T. z == x AND EXISTS u S.T. u == y";
		tracker = new SymbolTracker();
		test = new Processor( logicStatement , tracker );
		test.tokenize();
		tokens = getTokens( test );
		expected = Arrays.asList( Quantifier.FORALL , Symbol.LEFT_PAREN , tracker.getVariableByName( "x" ) , 
				Symbol.COMMA , tracker.getVariableByName( "y" ) , Symbol.RIGHT_PAREN , 
				Symbol.COMMA , Quantifier.EXISTS , tracker.getVariableByName( "z" ) ,
				Symbol.SUCH_THAT , tracker.getVariableByName( "z" ) , Operator.EQUALS ,
				tracker.getVariableByName( "x" ) , Operator.AND , Quantifier.EXISTS , 
				tracker.getVariableByName( "u" ) , Symbol.SUCH_THAT , tracker.getVariableByName( "u" ) ,
				Operator.EQUALS , tracker.getVariableByName( "y" ) );
		Assert.assertTrue( tokens.equals( expected ) );
		
		//test that != does not get mixed up with !
		logicStatement = "(!x OR y) && (x != y)";
		tracker = new SymbolTracker();
		test = new Processor( logicStatement , tracker );
		test.tokenize();
		tokens = getTokens( test );
		expected = Arrays.asList( Symbol.LEFT_PAREN , Operator.NOT , tracker.getVariableByName( "x" ) , 
							Operator.OR , tracker.getVariableByName( "y" ) , Symbol.RIGHT_PAREN ,
							Operator.AND , Symbol.LEFT_PAREN , tracker.getVariableByName( "x" ) , 
							Operator.NOT_EQUALS , tracker.getVariableByName( "y" ) , Symbol.RIGHT_PAREN );
		Assert.assertTrue( tokens.equals( expected ) );
		
		//test various operators
		logicStatement = "   && || ! != IMPLICATION BICONDITIONAL , AND OR NEQUALS EQUALS";
		tracker = new SymbolTracker();
		test = new Processor( logicStatement , tracker );
		test.tokenize();
		tokens = getTokens( test );
		expected = Arrays.asList( Operator.AND , Operator.OR , Operator.NOT , 
				Operator.NOT_EQUALS , Operator.IMPLICATION , Operator.BICONDITIONAL , 
				Symbol.COMMA , Operator.AND , Operator.OR , Operator.NOT_EQUALS , Operator.EQUALS );
		Assert.assertTrue( tokens.equals( expected ) );
		
		//test various symbols
		logicStatement = "   )   (   , S.T. ";
		tracker = new SymbolTracker();
		test = new Processor( logicStatement , tracker );
		test.tokenize();
		tokens = getTokens( test );
		expected = Arrays.asList( Symbol.RIGHT_PAREN , Symbol.LEFT_PAREN , Symbol.COMMA , Symbol.SUCH_THAT );
		Assert.assertTrue( tokens.equals( expected ) );
		
		//test various quantifiers
		logicStatement = "FORALL FORALL EXISTS    ";
		tracker = new SymbolTracker();
		test = new Processor( logicStatement , tracker );
		test.tokenize();
		tokens = getTokens( test );
		expected = Arrays.asList( Quantifier.FORALL , Quantifier.FORALL , Quantifier.EXISTS );
		Assert.assertTrue( tokens.equals( expected ) );
		
		//test various variable names
		logicStatement = "Xxx___28473 a1b2_c3d4 _";
		tracker = new SymbolTracker();
		test = new Processor( logicStatement , tracker );
		test.tokenize();
		tokens = getTokens( test );
		expected = Arrays.asList( tracker.getVariableByName( "Xxx___28473" ) , 
				tracker.getVariableByName( "a1b2_c3d4" ) ,
				tracker.getVariableByName( "_" ) );
		Assert.assertTrue( tokens.equals( expected ) );
		
		//test with numbers
		logicStatement = "FORALL x, x == 12345 OR x == 000.001";
		tracker = new SymbolTracker();
		test = new Processor( logicStatement , tracker );
		test.tokenize();
		tokens = getTokens( test );
		expected = Arrays.asList( Quantifier.FORALL , tracker.getVariableByName( "x" ) , 
				Symbol.COMMA , tracker.getVariableByName( "x" ) ,
				Operator.EQUALS , NumbersFOL.fromInt( 12345 ) , 
				Operator.OR , tracker.getVariableByName( "x" ) ,
				Operator.EQUALS , NumbersFOL.fromDouble( 0.001 ));
		Assert.assertTrue( tokens.equals( expected ) );
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void tokenizeInvalidVariableName() {
		Processor test;
		String logicStatement;
		SymbolTracker tracker = new SymbolTracker();
		List<Symbol> tokens;
		List<Symbol> expected;	
		
		logicStatement = "---bad_variable_name---";
		tracker = new SymbolTracker();
		test = new Processor( logicStatement , tracker );
		test.tokenize();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void tokenizeInvalidVariableName2() {
		Processor test;
		String logicStatement;
		SymbolTracker tracker = new SymbolTracker();
		List<Symbol> tokens;
		List<Symbol> expected;	
		
		logicStatement = "12345a";
		tracker = new SymbolTracker();
		test = new Processor( logicStatement , tracker );
		test.tokenize();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void tokenizeInvalidVariableName3() {
		Processor test;
		String logicStatement;
		SymbolTracker tracker = new SymbolTracker();
		List<Symbol> tokens;
		List<Symbol> expected;	
		
		logicStatement = "_12345a^";
		tracker = new SymbolTracker();
		test = new Processor( logicStatement , tracker );
		test.tokenize();
	}
	
	@Test
	public void tokenizeWithStructures() throws IOException {
		Processor test;
		String logicStatement;
		Object def = new IntegerWorld();
		SymbolTracker tracker = SymbolTracker.fromDataFile( "test/mjchao/mazenav/logic/structures/integerworld.txt" , def );
		List<Symbol> tokens;
		List<Symbol> expected;
		
		//basic acceptance test
		logicStatement = "FORALL(y) GreaterThan(y, 0)";
		def = new IntegerWorld();
		tracker = SymbolTracker.fromDataFile( "test/mjchao/mazenav/logic/structures/integerworld.txt" , def );
		test = new Processor( logicStatement , tracker );
		test.tokenize();
		tokens = getTokens( test );
		expected = Arrays.asList( Quantifier.FORALL , Symbol.LEFT_PAREN , 
				tracker.getVariableByName( "y" ) , Symbol.RIGHT_PAREN ,
				tracker.getRelation( "GreaterThan" ) , Symbol.LEFT_PAREN , 
				tracker.getVariableByName( "y" ) , Symbol.COMMA , 
				NumbersFOL.fromInt( 0 ) , Symbol.RIGHT_PAREN);
		Assert.assertTrue( tokens.equals( expected ) );
		
		//a bit more complicated statement
		logicStatement = "FORALL(x, y) GreaterThan(y, 0) => GreaterThan(SumInt(x,y), x)";
		def = new IntegerWorld();
		tracker = SymbolTracker.fromDataFile( "test/mjchao/mazenav/logic/structures/integerworld.txt" , def );
		test = new Processor( logicStatement , tracker );
		test.tokenize();
		tokens = getTokens( test );
		expected = Arrays.asList( Quantifier.FORALL , Symbol.LEFT_PAREN , 
				tracker.getVariableByName( "x" ) , Symbol.COMMA ,
				tracker.getVariableByName( "y" ) , Symbol.RIGHT_PAREN ,
				tracker.getRelation( "GreaterThan" ) , Symbol.LEFT_PAREN , 
				tracker.getVariableByName( "y" ) , Symbol.COMMA , 
				NumbersFOL.fromInt( 0 ) , Symbol.RIGHT_PAREN ,
				Operator.IMPLICATION , tracker.getRelation( "GreaterThan" ) ,
				Symbol.LEFT_PAREN , tracker.getFunction( "SumInt" ) ,
				Symbol.LEFT_PAREN , tracker.getVariableByName( "x" ) ,
				Symbol.COMMA , tracker.getVariableByName( "y" ) , 
				Symbol.RIGHT_PAREN , Symbol.COMMA ,
				tracker.getVariableByName( "x" ) , Symbol.RIGHT_PAREN );
		Assert.assertTrue( tokens.equals( expected ) );
		
		//check that undefined functions just become variables
		logicStatement = "FORALL(x, y) GreaterThan(y, 0) => GreaterThn(SumInt(x,y), x)";
		def = new IntegerWorld();
		tracker = SymbolTracker.fromDataFile( "test/mjchao/mazenav/logic/structures/integerworld.txt" , def );
		test = new Processor( logicStatement , tracker );
		test.tokenize();
		tokens = getTokens( test );
		expected = Arrays.asList( Quantifier.FORALL , Symbol.LEFT_PAREN , 
				tracker.getVariableByName( "x" ) , Symbol.COMMA ,
				tracker.getVariableByName( "y" ) , Symbol.RIGHT_PAREN ,
				tracker.getRelation( "GreaterThan" ) , Symbol.LEFT_PAREN , 
				tracker.getVariableByName( "y" ) , Symbol.COMMA , 
				NumbersFOL.fromInt( 0 ) , Symbol.RIGHT_PAREN ,
				Operator.IMPLICATION , tracker.getVariableByName( "GreaterThn" ) ,
				Symbol.LEFT_PAREN , tracker.getFunction( "SumInt" ) ,
				Symbol.LEFT_PAREN , tracker.getVariableByName( "x" ) ,
				Symbol.COMMA , tracker.getVariableByName( "y" ) , 
				Symbol.RIGHT_PAREN , Symbol.COMMA ,
				tracker.getVariableByName( "x" ) , Symbol.RIGHT_PAREN );
		Assert.assertTrue( tokens.equals( expected ) );
		
		//try using constant objects as well
		logicStatement = "AngleEquals( RightAngle , Angle(90) )";
		def = new GeometryWorld();
		tracker = SymbolTracker.fromDataFile( "test/mjchao/mazenav/logic/structures/geometryworld.txt" , def );
		test = new Processor( logicStatement , tracker );
		test.tokenize();
		tokens = getTokens( test );
		expected = Arrays.asList( tracker.getRelation( "AngleEquals" ) , Symbol.LEFT_PAREN ,
					tracker.getConstant( "RightAngle" ) , Symbol.COMMA ,
					tracker.getFunction( "Angle" ) , Symbol.LEFT_PAREN , 
					NumbersFOL.fromInt( 90 ) , Symbol.RIGHT_PAREN , Symbol.RIGHT_PAREN );
		Assert.assertTrue( tokens.equals( expected ) );
	}
}
