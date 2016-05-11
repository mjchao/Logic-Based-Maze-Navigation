package mjchao.mazenav.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mjchao.mazenav.logic.Resolver.Substitution;
import mjchao.mazenav.logic.StatementCNF.Disjunction;
import mjchao.mazenav.logic.StatementCNF.Disjunction.Term;
import mjchao.mazenav.logic.structures.BooleanFOL;
import mjchao.mazenav.logic.structures.Function;
import mjchao.mazenav.logic.structures.ObjectFOL;
import mjchao.mazenav.logic.structures.Relation;
import mjchao.mazenav.logic.structures.SymbolTracker;

import org.junit.Assert;
import org.junit.Test;

public class ResolverTest {

	@Test
	public void testUnifyVarBAT1() {
		//test unification with 2 variables that succeeds
		SymbolTracker tracker = new SymbolTracker();
		String infixTerms = "x AND y";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		List< Substitution > subs = Resolver.unifyVar( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs.toString().equals( "[?0/?1]" ) );
	}
	
	@Test
	public void testUnifyVarBAT2() {
		//test unification of a variable with a constant that succeeds
		SymbolTracker tracker = new SymbolTracker();
		String infixTerms = "x AND 100";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		List< Substitution > subs = Resolver.unifyVar( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs.toString().equals( "[?0/100]" ) );
	}
	
	@Test
	public void testUnifyVarBAT3() {
		//test unification of a variable with a constant that fails
		//because the variable was already unified with something else
		SymbolTracker tracker = new SymbolTracker();
		String infixTerms = "x AND 100 AND 5";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		Term x = terms.get( 0 );
		Term num100 = terms.get( 1 );
		Term num5 = terms.get( 2 );
		
		Substitution s1 = new Substitution( x , num5 );
		List< Substitution > prevSubs = new ArrayList< Substitution >();
		prevSubs.add( s1 );
		
		List< Substitution > subs = Resolver.unifyVar( x , num100 , prevSubs );
		Assert.assertTrue( subs == null );
	}
	
	@Test
	public void testUnifyVarBAT4() {
		//test a repeat unification where we unify a variable again
		//with the same value. this should succeed, but it should not
		//generate a new substitution
		SymbolTracker tracker = new SymbolTracker();
		String infixTerms = "x AND 100";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		Term x = terms.get( 0 );
		Term num100 = terms.get( 1 );
		
		Substitution s1 = new Substitution( x , num100 );
		List< Substitution > prevSubs = new ArrayList< Substitution >();
		prevSubs.add( s1 );
		
		List< Substitution > subs = Resolver.unifyVar( x , num100 , prevSubs );
		Assert.assertTrue( subs.toString().equals( "[?0/100]" ) );
	}
	
	@Test
	public void testUnifyVarBAT5() {
		//test propagating substitutions. we unify y with 100 and then x with y
		//so the final substitutions should be "y/100" and "x/100"
		//(note that this wouldn't happen if we unified x with y and then y
		//with 100).
		SymbolTracker tracker = new SymbolTracker();
		String infixTerms = "x AND y AND 100";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		Term x = terms.get( 0 );
		Term y = terms.get( 1 );
		Term num100 = terms.get( 2 );
		
		Substitution s1 = new Substitution( y , num100 );
		List< Substitution > prevSubs = new ArrayList< Substitution >();
		prevSubs.add( s1 );
		
		List< Substitution > subs = Resolver.unifyVar( x , y , prevSubs );
		Assert.assertTrue( subs.toString().equals( "[?1/100, ?0/100]" ) );
	}
	
	@Test
	public void testUnifyVarOccurCheck1(){
		//test unifying a variable with its own negation. This should fail.
		//i.e. unifying x with !x should fail.
		SymbolTracker tracker = new SymbolTracker();
		String infixTerms = "x AND !x";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		Term x = terms.get( 0 );
		Term y = terms.get( 1 );
		
		List< Substitution > subs = Resolver.unifyVar( x , y , new ArrayList< Substitution >() );
		Assert.assertTrue( subs == null );
	}
	
	/**
	 * Mock class for testing with functions
	 */
	public static class FunctionTester {
		
		public ObjectFOL Func1( ObjectFOL arg1 ) {
			return null;
		}
		
		public ObjectFOL Func2( ObjectFOL arg1 , ObjectFOL arg2 ) {
			return null;
		}
		
		public ObjectFOL Func3( ObjectFOL arg1 , ObjectFOL arg2 , ObjectFOL arg3 ) {
			return null;
		}
		
		public static SymbolTracker buildTracker() {
			SymbolTracker tracker = new SymbolTracker();
			FunctionTester definingInstance = new FunctionTester();
			Function Func1 = new Function( "Func1" , definingInstance , "Object" );
			tracker.addFunction( "Func1" , Func1 );
			
			Function Func2 = new Function( "Func2" , definingInstance , "Object" , "Object" );
			tracker.addFunction( "Func2" , Func2 );
			
			Function Func3 = new Function( "Func3" , definingInstance , "Object" , "Object" , "Object" );
			tracker.addFunction( "Func3" , Func3 );
			
			return tracker;
		}
	}
	
	@Test
	public void testUnifyFunctions1() {
		//test unifying variables within functions
		SymbolTracker tracker = FunctionTester.buildTracker();
		String infixTerms = "Func3(u,v,w) AND Func3(x,y,z)";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs.toString().equals( "[?0/?3, ?1/?4, ?2/?5]" ) );
	}
	
	@Test
	public void testUnifyFunctions2() {
		//test unifying a variable in a function with another function
		SymbolTracker tracker = FunctionTester.buildTracker();
		String infixTerms = "Func3(u,v,w) AND Func3(Func1(x),y,z)";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs.toString().equals( "[?0/Func1(?3), ?1/?4, ?2/?5]" ) );
	}
	
	@Test
	public void testUnifyFunctions3() {
		//test unifying a function with a variable (i.e. the algorithm needs
		//to flip it around and unify the variable with the function)
		SymbolTracker tracker = FunctionTester.buildTracker();
		String infixTerms = "Func3(Func1(u),v,w) AND Func3(x,y,z)";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs.toString().equals( "[?3/Func1(?0), ?1/?4, ?2/?5]" ) );
	}
	
	@Test
	public void testUnifyFunctions4() {
		//test unifying with multiply-nested functions
		SymbolTracker tracker = FunctionTester.buildTracker();
		String infixTerms = "Func3(Func3(Func1(a),Func1(b),Func1(c)),Func1(d),Func2(Func1(e),f)) AND Func3(Func3(u,v,Func1(w)),x,y)";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs.toString().equals( "[?6/Func1(?0), ?7/Func1(?1), ?2/?8, ?9/Func1(?3), ?10/Func2(Func1(?4), ?5)]" ) );
	}
	
	@Test
	public void testUnifyFunctions5() {
		//test failed unification
		SymbolTracker tracker = FunctionTester.buildTracker();
		String infixTerms = "Func3(a,b,Func1(c)) AND Func3(u,v,Func2(w,x))";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs == null );
	}
	
	@Test
	public void testUnifyFunctions6() {
		//test failed unification
		SymbolTracker tracker = FunctionTester.buildTracker();
		String infixTerms = "Func3(Func3(Func1(a),Func1(b),Func1(c)),Func1(d),Func2(Func1(e),f)) AND Func3(Func3(u,v,Func1(w)),Func2(x,y),z)";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs == null );
	}
	
	//-----test cases for unify with functions and previous substitutions-----//
	
	@Test
	public void testUnifyFunctions7() {
		//test unification for functions with previous substitutions
		//here, we substitute f/a and then we check if the substitution
		//b/f propagates to the substitution b/a
		SymbolTracker tracker = FunctionTester.buildTracker();
		String infixTerms = "Func3(Func1(a),Func1(b),Func1(c)) AND Func3(d,e,Func1(f))";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		
		Term Func1b = terms.get( 0 ).getArgs()[ 1 ];
		Term b = Func1b.getArgs()[ 0 ];
		Term f = terms.get( 1 ).getArgs()[ 2 ].getArgs()[ 0 ];
		
		List<Substitution> prevSubs = new ArrayList< Substitution >();
		Substitution prevSub1 = new Substitution( f , b );
		prevSubs.add( prevSub1 );
		
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , prevSubs );
		Assert.assertTrue( subs.toString().equals( "[?5/?1, ?3/Func1(?0), ?4/Func1(?1), ?2/?1]" ) );
	}
	
	@Test
	public void testUnifyFunctions8() {
		//test unification for functions with previous substitutions
		//here, we substitute c/Func1(b) and then we check if the 
		//substitution f/c resolves to f/Func1(b), which resolves to
		//c/Func1(Func1(b))
		SymbolTracker tracker = FunctionTester.buildTracker();
		String infixTerms = "Func3(Func1(a),Func1(b),c) AND Func3(d,e,f)";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		
		Term Func1b = terms.get( 0 ).getArgs()[ 1 ];
		Term c = terms.get( 0 ).getArgs()[ 2 ];
		
		List<Substitution> prevSubs = new ArrayList< Substitution >();
		Substitution prevSub1 = new Substitution( c , Func1b );
		prevSubs.add( prevSub1 );
		
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , prevSubs );
		Assert.assertTrue( subs.toString().equals( "[?2/Func1(?1), ?3/Func1(?0), ?4/Func1(?1), ?5/Func1(?1)]" ) );
	}
	
	//--------------test cases for unify with skolem functions----------------//
	
	@Test
	public void testUnifySkolemBAT1() {
		//test simple unification of a variable with a skolem function
		//that takes no arguments
		SymbolTracker tracker = new SymbolTracker();
		String infixTerms = "EXISTS(x) x AND y";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs.toString().equals( "[?1/$0()]" ) );
	}
	
	@Test
	public void testUnifySkolemBAT2() {
		//test simple unification of a variable with a skolem function
		//that takes some arguments
		SymbolTracker tracker = new SymbolTracker();
		String infixTerms = "FORALL(x,y,z) EXISTS(x) x AND w";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs.toString().equals( "[?4/$0(?0, ?1, ?2)]" ) );
	}
	
	@Test
	public void testUnifySkolemBAT3() {
		//test simple unification of two skolem functions
		SymbolTracker tracker = new SymbolTracker();
		String infixTerms = "EXISTS(x) x AND EXISTS(y) y";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs.toString().equals( "[$0()/$1()]" ) );
	}
	
	@Test
	public void testUnifySkolemBAT4() {
		//test occur check for trying to unify a variable with a skolem function
		//that takes the variable as an argument. This should fail. To see why,
		//consider the case where we try to unify the x and y in FORALL(y) EXISTS(x).
		//EXISTS(x) is basically a function that returns a value based on what y
		//is. Trying to unify y with this function creates a circular dependency
		//that cannot be resolved.
		SymbolTracker tracker = new SymbolTracker();
		String infixTerms = "FORALL(y) EXISTS(x) x AND y";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs == null );
	}
	
	@Test
	public void testUnifySkolemWithArgs() {
		//test simple unification of two skolem functions
		SymbolTracker tracker = new SymbolTracker();
		String infixTerms = "(FORALL(a,b,c) EXISTS(x) x) AND (FORALL(d,e,f) EXISTS(y) y)";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , new ArrayList< Substitution >() );
		Assert.assertTrue( subs.toString().equals( "[$0(?0, ?1, ?2)/$1(?4, ?5, ?6)]" ) );
	}
	
	@Test
	public void testUnifySkolemPrevSubs1() {
		//test simple unification of two skolem functions with previous substitutions
		//previous substitutions should not really have an effect although the
		//variables appear as skolem function arguments because we don't propagate
		//variable substitutions for arguments to skolem functions at this stage yet.
		SymbolTracker tracker = new SymbolTracker();
		String infixTerms = "(FORALL(a,b,c) EXISTS(x) x) AND (FORALL(d,e,f) EXISTS(y) y) AND k AND m AND n";
		List< Term > terms = StatementCNFTest.termsListFromInfix( infixTerms , tracker );
		Term a = new Term( tracker.getSystemVariableById( 0 ) , false );
		Term b = new Term( tracker.getSystemVariableById( 1 ) , false );
		Term c = new Term( tracker.getSystemVariableById( 2 ), false );
		Term k = terms.get( 2 );
		Term m = terms.get( 3 );
		Term n = terms.get( 4 );
		List< Substitution > prevSubs = new ArrayList< Substitution >();
		Substitution prevSub1 = new Substitution( a ,k );
		prevSubs.add( prevSub1 );
		Substitution prevSub2 = new Substitution( b , m );
		prevSubs.add( prevSub2 );
		Substitution prevSub3 = new Substitution( c , n );
		prevSubs.add( prevSub3 );
		List< Substitution > subs = Resolver.unify( terms.get( 0 ) , terms.get( 1 ) , prevSubs );
		Assert.assertTrue( subs.toString().equals( "[?0/?8, ?1/?9, ?2/?10, $0(?0, ?1, ?2)/$1(?4, ?5, ?6)]" ) );
	}
	
	//-----------------end unification test cases-----------------------------//
	
	@Test
	public void testResolveBAT1() {
		//test resolving "A" with "!A", which should yield an empty disjunction
		SymbolTracker tracker = new SymbolTracker();
		String infix = "A AND !A";
		List< Disjunction > disjunctions = StatementCNFTest.disjunctionsFromInfix( infix , tracker );
		List< Disjunction > expected = Arrays.asList( new Disjunction() );
		List< Disjunction > resolveClauses = Resolver.resolve( disjunctions.get( 0 ) , disjunctions.get( 1 ) );
		Assert.assertTrue( expected.equals( resolveClauses ) );
	}
	
	@Test
	public void testResolveBAT2() {
		//test resolving "T" with "F" (true with false), which should not result in anything 
		//resolvable
		SymbolTracker tracker = new SymbolTracker();
		String infix = "T AND F";
		List< Disjunction > disjunctions = StatementCNFTest.disjunctionsFromInfix( infix , tracker );
		List< Disjunction > expected = new ArrayList< Disjunction >();
		List< Disjunction > resolveClauses = Resolver.resolve( disjunctions.get( 0 ) , disjunctions.get( 1 ) );
		Assert.assertEquals( expected , resolveClauses );
	}
	
	@Test
	public void testResolveBAT3() {
		//test resolving "A" with "!B", which should result in failed
		//unification because a variable cannot substitute for a different
		//negated variable
		SymbolTracker tracker = new SymbolTracker();
		String infix = "A AND !B";
		List< Disjunction > disjunctions = StatementCNFTest.disjunctionsFromInfix( infix , tracker );
		List< Disjunction > expected = new ArrayList< Disjunction >();
		List< Disjunction > resolveClauses = Resolver.resolve( disjunctions.get( 0 ) , disjunctions.get( 1 ) );
		Assert.assertEquals( expected , resolveClauses );
	}
	
	@Test
	public void testResolveBAT4() {
		//test resolving "A" with "B", which should result in an empty disjunction
		//because "A" and "B" are different variables and should not 
		//resolve to anything
		SymbolTracker tracker = new SymbolTracker();
		String infix = "A AND B";
		List< Disjunction > disjunctions = StatementCNFTest.disjunctionsFromInfix( infix , tracker );
		List< Disjunction > expected = new ArrayList< Disjunction >();
		List< Disjunction > resolveClauses = Resolver.resolve( disjunctions.get( 0 ) , disjunctions.get( 1 ) );
		Assert.assertEquals( expected , resolveClauses );
	}
	
	//TODO test with skolem functions
	
	@Test
	public void testProveHypothesisBAT1() {
		//basic modus ponens:
		//P => Q, P
		//---------			should yield true
		//	  Q
		SymbolTracker tracker = new SymbolTracker();
		StatementCNF kb1 = StatementCNF.fromInfixString( "P => Q" , tracker );
		StatementCNF kb2 = StatementCNF.fromInfixString( "P" , tracker );
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Q" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb1 , kb2 ) );
	}
	
	@Test
	public void testProveHypothesisBAT2() {
		//basic modus ponens:
		//P => Q, P
		//---------			should yield false
		//	  !Q
		SymbolTracker tracker = new SymbolTracker();
		StatementCNF kb1 = StatementCNF.fromInfixString( "P => Q" , tracker );
		StatementCNF kb2 = StatementCNF.fromInfixString( "P" , tracker );
		StatementCNF hypothesis = StatementCNF.fromInfixString( "!Q" , tracker );
		Assert.assertFalse( Resolver.proveHypothesis( tracker , hypothesis , kb1 , kb2 ) );
	}
	
	@Test
	public void testProveHypothesisBAT3() {
		//test using <=> which will result in many more disjunctions
		//which makes the resolution algorithm deal with many more clauses
		//P <=> Q, P
		//---------			should yield true
		//	  Q
		SymbolTracker tracker = new SymbolTracker();
		StatementCNF kb1 = StatementCNF.fromInfixString( "P <=> Q" , tracker );
		StatementCNF kb2 = StatementCNF.fromInfixString( "P" , tracker );
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Q" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb1 , kb2 ) );
	}
	
	@Test
	public void testProveHypothesisBAT4() {
		//test using <=> which will result in many more disjunctions
		//which makes the resolution algorithm deal with many more clauses
		//P <=> Q, P
		//---------			should yield false
		//	  !Q
		SymbolTracker tracker = new SymbolTracker();
		StatementCNF kb1 = StatementCNF.fromInfixString( "P <=> Q" , tracker );
		StatementCNF kb2 = StatementCNF.fromInfixString( "P" , tracker );
		StatementCNF hypothesis = StatementCNF.fromInfixString( "!Q" , tracker );
		Assert.assertFalse( Resolver.proveHypothesis( tracker , hypothesis , kb1 , kb2 ) );
	}
	
	@Test
	public void testProveHypothesisBAT5() {
		//test using irrelevant information
		//P => Q, P
		//---------			should yield true, but P => Q is irrelevant
		//	  P
		SymbolTracker tracker = new SymbolTracker();
		StatementCNF kb1 = StatementCNF.fromInfixString( "P => Q" , tracker );
		StatementCNF kb2 = StatementCNF.fromInfixString( "P" , tracker );
		StatementCNF hypothesis = StatementCNF.fromInfixString( "P" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb1 , kb2 ) );
	}
	
	@Test
	public void testProveHypothesisBAT6() {
		//test chaining implications
		//P => Q, Q => R, R => S , P
		//--------------------------			should yield true
		//	  		 S
		SymbolTracker tracker = new SymbolTracker();
		StatementCNF kb1 = StatementCNF.fromInfixString( "P => Q" , tracker );
		StatementCNF kb2 = StatementCNF.fromInfixString( "Q => R" , tracker );
		StatementCNF kb3 = StatementCNF.fromInfixString( "R => S" , tracker );
		StatementCNF kb4 = StatementCNF.fromInfixString( "P" , tracker );
		StatementCNF hypothesis = StatementCNF.fromInfixString( "S" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb1 , kb2 , kb3 , kb4 ) );
	}
	
	@Test
	public void testProveHypothesisBAT7() {
		//test chaining implications
		//P => Q, Q => R, R => S , P
		//--------------------------			should yield false
		//	  		 !S
		SymbolTracker tracker = new SymbolTracker();
		StatementCNF kb1 = StatementCNF.fromInfixString( "P => Q" , tracker );
		StatementCNF kb2 = StatementCNF.fromInfixString( "Q => R" , tracker );
		StatementCNF kb3 = StatementCNF.fromInfixString( "R => S" , tracker );
		StatementCNF kb4 = StatementCNF.fromInfixString( "P" , tracker );
		StatementCNF hypothesis = StatementCNF.fromInfixString( "!S" , tracker );
		Assert.assertFalse( Resolver.proveHypothesis( tracker , hypothesis , kb1 , kb2 , kb3 , kb4 ) );
	}
	
	@Test
	public void testProveHypothesisBAT8() {
		//test with extra irrelevant information
		//P => Z , P => Q , Q => A Q => R, 
		//R => B , R => S S <=> C , P
		//----------------------------			should yield true
		//	  		 S
		SymbolTracker tracker = new SymbolTracker();
		StatementCNF kb1 = StatementCNF.fromInfixString( "P => Z" , tracker );
		StatementCNF kb2 = StatementCNF.fromInfixString( "P => Q" , tracker );
		StatementCNF kb3 = StatementCNF.fromInfixString( "Q => A" , tracker );
		StatementCNF kb4 = StatementCNF.fromInfixString( "Q => R" , tracker );
		StatementCNF kb5 = StatementCNF.fromInfixString( "R => B" , tracker );
		StatementCNF kb6 = StatementCNF.fromInfixString( "R => S" , tracker );
		StatementCNF kb7 = StatementCNF.fromInfixString( "S <=> C" , tracker );
		StatementCNF kb8 = StatementCNF.fromInfixString( "P" , tracker );
		StatementCNF hypothesis = StatementCNF.fromInfixString( "S" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb1 , kb2 , kb3 , kb4 , kb5 , kb6 , kb7 , kb8 ) );
	}
	
	@Test
	public void testProveHypothesisBAT9() {
		//test chaining information from multiple sources
		//A => B , B => C, 	-
		//K => L , L => M,	 |--> C AND M AND R => Z
		//P => Q , Q => R, 	-
		//A, K, P
		//----------------------------					should yield true
		//	  		 Z
		SymbolTracker tracker = new SymbolTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "A => B" , tracker ) ,
			StatementCNF.fromInfixString( "B => C" , tracker ) ,
			StatementCNF.fromInfixString( "K => L" , tracker ) ,
			StatementCNF.fromInfixString( "L => M" , tracker ) ,
			StatementCNF.fromInfixString( "P => Q" , tracker ) ,
			StatementCNF.fromInfixString( "Q => R" , tracker ) ,
			StatementCNF.fromInfixString( "C AND M AND R => Z" , tracker ) ,
			StatementCNF.fromInfixString( "A" , tracker ) ,
			StatementCNF.fromInfixString( "K" , tracker ) ,
			StatementCNF.fromInfixString( "P" , tracker )
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Z" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void testProveHypothesisBAT10() {
		//test chaining information from multiple sources
		//A => B , B => C, 	-
		//K => L , L => M,	 |--> C AND M AND R => Z
		//P => Q , Q => R, 	-
		//A, K, P
		//----------------------------					should yield true
		//	  		 Z
		SymbolTracker tracker = new SymbolTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "A => B" , tracker ) ,
			StatementCNF.fromInfixString( "B => C" , tracker ) ,
			StatementCNF.fromInfixString( "K => L" , tracker ) ,
			StatementCNF.fromInfixString( "L => M" , tracker ) ,
			StatementCNF.fromInfixString( "P => Q" , tracker ) ,
			StatementCNF.fromInfixString( "Q => R" , tracker ) ,
			StatementCNF.fromInfixString( "C AND M AND R => Z" , tracker ) ,
			StatementCNF.fromInfixString( "!A" , tracker ) ,
			StatementCNF.fromInfixString( "!K" , tracker ) ,
			StatementCNF.fromInfixString( "!P" , tracker )
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Z" , tracker );
		Assert.assertFalse( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	/**
	 * Mock class for testing with functions and relations
	 */
	public static class FunctionRelationTester {
		
		final public ObjectFOL obj1 = new ObjectFOL( "obj1" , null , "Object" );
		public ObjectFOL obj1() {
			return obj1;
		}
		
		final public ObjectFOL obj2 = new ObjectFOL( "obj2" , null , "Object" );
		public ObjectFOL obj2() {
			return obj2;
		}
		
		final public ObjectFOL obj3 = new ObjectFOL( "obj3" , null , "Object" );
		public ObjectFOL obj3() {
			return obj3;
		}
		
		public ObjectFOL Func1( ObjectFOL arg1 ) {
			return null;
		}
		
		public ObjectFOL Func2( ObjectFOL arg1 , ObjectFOL arg2 ) {
			return null;
		}
		
		public ObjectFOL Func3( ObjectFOL arg1 , ObjectFOL arg2 , ObjectFOL arg3 ) {
			return null;
		}
		
		public BooleanFOL Rel1( ObjectFOL arg1 ){
			return null;
		}
		
		public BooleanFOL Rel2( ObjectFOL arg1 , ObjectFOL arg2 ) {
			return null;
		}
		
		public BooleanFOL Rel3( ObjectFOL arg1 , ObjectFOL arg2 , ObjectFOL arg3 ) {
			return null;
		}
		
		public static SymbolTracker buildTracker() {
			SymbolTracker tracker = new SymbolTracker();
			FunctionRelationTester definingInstance = new FunctionRelationTester();
			Function Func1 = new Function( "Func1" , definingInstance , "Object" );
			tracker.addFunction( "Func1" , Func1 );
			
			Function Func2 = new Function( "Func2" , definingInstance , "Object" , "Object" );
			tracker.addFunction( "Func2" , Func2 );
			
			Function Func3 = new Function( "Func3" , definingInstance , "Object" , "Object" , "Object" );
			tracker.addFunction( "Func3" , Func3 );
			
			Relation Rel1 = new Relation( "Rel1" , definingInstance , "Object" );
			tracker.addRelation( "Rel1" , Rel1 );
			
			Relation Rel2 = new Relation( "Rel2" , definingInstance , "Object" , "Object" );
			tracker.addRelation( "Rel2" , Rel2 );
			
			Relation Rel3 = new Relation( "Rel3" , definingInstance , "Object"  , "Object" , "Object" );
			tracker.addRelation( "Rel3" , Rel3 );
			
			Function obj1 = new Function( "obj1" , definingInstance );
			tracker.addConstant( "obj1" , obj1 );
			
			Function obj2 = new Function( "obj2" , definingInstance );
			tracker.addConstant( "obj2" , obj2 );
			
			Function obj3 = new Function( "obj3" , definingInstance );
			tracker.addConstant( "obj3" , obj3 );
			
			return tracker;
		}
	}
	
	@Test
	public void testProveHypothesisFunctionsBAT1() {
		//test basic modus ponens with functions and relations
		//Rel1(obj1) => Rel1(obj2), Rel1(obj1)
		//----------------------------			should yield true
		//	  		 Rel1(obj2)
		SymbolTracker tracker = FunctionRelationTester.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "Rel1(obj1) => Rel1(obj2)" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj1)" , tracker )
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Rel1(obj2)" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void testProveHypothesisFunctionsBAT2() {
		//test basic modus ponens with functions and relations
		//Rel1(obj1) => Rel1(obj2), Rel1(obj1)
		//----------------------------			should yield false
		//	  		 !Rel1(obj2)
		SymbolTracker tracker = FunctionRelationTester.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "Rel1(obj1) => Rel1(obj2)" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj1)" , tracker )
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "!Rel1(obj2)" , tracker );
		Assert.assertFalse( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void testProveHypothesisFunctionsBAT3() {
		//test proofs involving a biconditional, which should yield
		//multiple disjunctions
		//Rel1(obj1) <=> Rel1(obj2), Rel1(obj1)
		//------------------------------------		should yield true
		//            Rel1(obj2)
		SymbolTracker tracker = FunctionRelationTester.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "Rel1(obj1) <=> Rel1(obj2)" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj1)" , tracker )
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Rel1(obj2)" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void testProveHypothesisFunctionsBAT4() {
		//test proofs involving a biconditional, which should yield
		//multiple disjunctions
		//Rel1(obj1) <=> Rel1(obj2), Rel1(obj1)
		//------------------------------------		should yield false
		//            !Rel1(obj2)
		SymbolTracker tracker = FunctionRelationTester.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "Rel1(obj1) <=> Rel1(obj2)" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj1)" , tracker )
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "!Rel1(obj2)" , tracker );
		Assert.assertFalse( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void testProveHypothesisFunctionsBAT5() {
		//test with irrelevant information
		//Rel1(obj1) => Rel1(obj2), Rel1(obj1)
		//------------------------------------        should yield true
		//         Rel1(obj1)
		SymbolTracker tracker = FunctionRelationTester.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "Rel1(obj1) => Rel1(obj2)" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj1)" , tracker )
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Rel1(obj1)" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void testProveHypothesisFunctionsBAT6() {
		//test with irrelevant information
		//Rel1(obj1) => Rel1(obj2), Rel1(obj1)
		//------------------------------------        should yield true
		//         !Rel1(obj1)
		SymbolTracker tracker = FunctionRelationTester.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "Rel1(obj1) => Rel1(obj2)" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj1)" , tracker )
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "!Rel1(obj1)" , tracker );
		Assert.assertFalse( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void testProveHypothesisFunctionsBAT7() {
		//test chaining implications that have been shuffled. The correct
		//sequence should be Rel1(obj1) --> Rel1(obj2) --> Rel1(obj3) ---> Rel2(obj1)
		//Rel1(obj2) => Rel1(obj3) , Rel1(obj3) => Rel2(obj1,obj2) , Rel1(obj1) => Rel1(obj2), Rel1(obj1)
		//-------------------------------------------------------------------------------------------  should yield true
		//                                 Rel2(obj1,obj2)
		SymbolTracker tracker = FunctionRelationTester.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "Rel1(obj2) => Rel1(obj3)" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj3) => Rel2(obj1,obj2)", tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj1) => Rel1(obj2)" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj1)" , tracker )
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Rel2(obj1,obj2)" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void testProveHypothesisFunctionsBAT8() {
		//test chaining implications that have been shuffled. The correct
		//sequence should be Rel1(obj1) --> Rel1(obj2) --> Rel1(obj3) ---> Rel2(obj1)
		//so !Rel2(obj1,obj2) should be impossible to prove
		//Rel1(obj2) => Rel1(obj3) , Rel1(obj3) => Rel2(obj1,obj2) , Rel1(obj1) => Rel1(obj2), Rel1(obj1)
		//------------------------------------------------------------------------------------------------ should yield true
		//                                 !Rel2(obj1,obj2)
		SymbolTracker tracker = FunctionRelationTester.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "Rel1(obj2) => Rel1(obj3)" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj3) => Rel2(obj1,obj2)", tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj1) => Rel1(obj2)" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj1)" , tracker )
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "!Rel2(obj1,obj2)" , tracker );
		Assert.assertFalse( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void testProveHypothesisFunctionsNested1() {
		//test proving hypotheses with nested arguments
		//that are functions
		//Rel1(x) => Rel2(x,x), Rel1(Func1(obj1))
		//-------------------------------------    should yield true
		//	         Rel2(Func1(obj1),Func1(obj1))
		SymbolTracker tracker = FunctionRelationTester.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "Rel1(x) => Rel2(x,x)" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(Func1(obj1))", tracker ) ,
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Rel2(Func1(obj1),Func1(obj1))" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void testProveHypothesisFunctionsNested1F() {
		//test proving hypotheses with nested arguments
		//that are functions
		//Rel1(x) => Rel2(x,Func1(x)), Rel1(Func1(obj1))
		//-------------------------------------------------    should yield false
		//	         Rel2(Func1(obj1),Func1(obj1))
		SymbolTracker tracker = FunctionRelationTester.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "Rel1(x) => Rel2(x,Func1(x))" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(Func1(obj1))", tracker ) ,
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Rel2(Func1(obj1),Func1(obj1))" , tracker );
		Assert.assertFalse( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void testProveHypothesisFunctionsNested2T() {
		//test proving hypotheses with nested arguments
		//that are functions
		//Rel1(x) <=> Rel2(x,Func1(x)), Rel2(x,Func1(x)) => Rel3(x,Func1(x),Func1(x)),
		//Rel3(x,Func1(x),Func1(x)) => Rel3(x,Func2(x,x),Func2(x,x)), Rel1(obj2)
		//----------------------------------------------------------------------------- should yield true
		//                         Rel3(obj2,Func2(obj2,obj2),Func2(obj2,obj2))
		SymbolTracker tracker = FunctionRelationTester.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "Rel1(x) <=> Rel2(x,Func1(x))" , tracker ) ,
			StatementCNF.fromInfixString( "Rel2(x,Func1(x)) => Rel3(x,Func1(x),Func1(x))" , tracker ) ,
			StatementCNF.fromInfixString( "Rel3(x,Func1(x),Func1(x)) => Rel3(x,Func2(x,x),Func2(x,x))" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj2)", tracker ) ,
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Rel3(obj2,Func2(obj2,obj2),Func2(obj2,obj2))" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void testProveHypothesisFunctionsNested2F() {
		//test proving hypotheses with nested arguments
		//that are functions
		//Rel1(x) <=> Rel2(x,Func1(x)), Rel2(x,Func1(x)) => Rel3(x,Func1(x),Func1(x)),
		//Rel3(x,Func1(x),Func1(x)) => Rel3(Func1(x),Func2(x,x),Func2(x,x)), Rel1(obj2)
		//----------------------------------------------------------------------------- should yield false
		//                         Rel3(obj2,Func2(obj2,obj2),Func2(obj2,obj2))
		SymbolTracker tracker = FunctionRelationTester.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "Rel1(x) <=> Rel2(x,Func1(x))" , tracker ) ,
			StatementCNF.fromInfixString( "Rel2(x,Func1(x)) => Rel3(x,Func1(x),Func1(x))" , tracker ) ,
			StatementCNF.fromInfixString( "Rel3(x,Func1(x),Func1(x)) => Rel3(Func1(x),Func2(x,x),Func2(x,x))" , tracker ) ,
			StatementCNF.fromInfixString( "Rel1(obj2)", tracker ) ,
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Rel3(obj2,Func2(obj2,obj2),Func2(obj2,obj2))" , tracker );
		Assert.assertFalse( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	public static class Integration1 {
		
		final public static ObjectFOL Nono = new ObjectFOL( "Nono" , null , "Object" , "Nation" );
		final public ObjectFOL Nono() {
			return Nono;
		}
		
		final public static ObjectFOL M1 = new ObjectFOL( "M1" , null , "Object" , "Missile" );
		final public ObjectFOL M1() {
			return M1;
		}
		
		final public static ObjectFOL West = new ObjectFOL( "West" , null , "Object" , "American" );
		final public ObjectFOL West() {
			return West;
		}
		
		public BooleanFOL American( ObjectFOL arg0 ) {
			return null;
		}
		
		public BooleanFOL Missile( ObjectFOL arg0 ) {
			return null;
		}
		
		//Functions and Relations that do not need to be defined:
		public BooleanFOL Weapon( ObjectFOL arg0 ) {
			return null;
		}
		
		public BooleanFOL Sells( ObjectFOL arg0 , ObjectFOL arg1 , ObjectFOL arg2 ) {
			return null;
		}
		
		public BooleanFOL Hostile( ObjectFOL arg0 ) {
			return null;
		}
		
		public BooleanFOL Criminal( ObjectFOL arg0 ) {
			return null;
		}
		
		public BooleanFOL Owns( ObjectFOL arg0 , ObjectFOL arg1 ) {
			return null;
		}
		
		public BooleanFOL Enemy( ObjectFOL arg0 , ObjectFOL arg1 ) {
			return null;
		}
		
		public static SymbolTracker buildTracker() {
			SymbolTracker rtn = new SymbolTracker();
			Integration1 definingInstance = new Integration1();
			
			Function Nono = new Function( "Nono" , definingInstance );
			rtn.addConstant( "Nono" , Nono );
			
			Function M1 = new Function( "M1" , definingInstance );
			rtn.addConstant( "M1" , M1 );
			
			Function West = new Function( "West" , definingInstance );
			rtn.addConstant( "West" , West );
			
			Relation American = new Relation( "American" , definingInstance , "Object" );
			rtn.addRelation( "American" , American );
			
			Relation Missile = new Relation( "Missile" , definingInstance , "Object" );
			rtn.addRelation( "Missile" , Missile );
			
			Relation Weapon = new Relation( "Weapon" , definingInstance , "Object" );
			rtn.addRelation( "Weapon" , Weapon );
			
			Relation Sells = new Relation( "Sells" , definingInstance , "Object" , "Object" , "Object" );
			rtn.addRelation( "Sells" , Sells );
			
			Relation Hostile = new Relation( "Hostile" , definingInstance , "Object" );
			rtn.addRelation( "Hostile" , Hostile );
			
			Relation Criminal = new Relation( "Criminal" , definingInstance , "Object" );
			rtn.addRelation( "Criminal" , Criminal );
			
			Relation Owns = new Relation( "Owns" , definingInstance , "Object" , "Object" );
			rtn.addRelation( "Owns" , Owns );
			
			Relation Enemy = new Relation( "Enemy" , definingInstance , "Object" , "Object" );
			rtn.addRelation( "Enemy" , Enemy );
			
			return rtn;
		}
	}
	
	@Test
	public void integration1T() {
		//apply the example used by Russell and Norvig on page 330-331
		//(the resolution proof is on page 348)
		SymbolTracker tracker = Integration1.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "American(x) AND Weapon(y) AND Sells(x,y,z) AND Hostile(z) => Criminal(x)" , tracker ) ,
			StatementCNF.fromInfixString( "Owns(Nono,M1)" , tracker ) ,
			StatementCNF.fromInfixString( "Missile(M1)" , tracker) ,
			StatementCNF.fromInfixString( "Missile(x) AND Owns(Nono,x) => Sells(West,x,Nono)", tracker ) ,
			StatementCNF.fromInfixString( "Missile(x) => Weapon(x)" , tracker ) ,
			StatementCNF.fromInfixString( "Enemy(x, America) => Hostile(x)", tracker ) ,
			StatementCNF.fromInfixString( "American(West)" , tracker ) ,
			StatementCNF.fromInfixString( "Enemy(Nono, America)" , tracker )
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Criminal(West)" , tracker );
		Assert.assertTrue( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
	
	@Test
	public void integration1F() {
		//apply the example used by Russell and Norvig on page 330-331
		//(the resolution proof is on page 348)
		
		//test for potential positive-bias:
		//if we remove the fact that missiles are weapons, it should no longer
		//be possible to prove that west is a criminal
		SymbolTracker tracker = Integration1.buildTracker();
		StatementCNF[] kb = new StatementCNF[] {
			StatementCNF.fromInfixString( "American(x) AND Weapon(y) AND Sells(x,y,z) AND Hostile(z) => Criminal(x)" , tracker ) ,
			StatementCNF.fromInfixString( "Owns(Nono,M1)" , tracker ) ,
			StatementCNF.fromInfixString( "Missile(M1)" , tracker) ,
			StatementCNF.fromInfixString( "Missile(x) AND Owns(Nono,x) => Sells(West,x,Nono)", tracker ) ,
			//StatementCNF.fromInfixString( "Missile(x) => Weapon(x)" , tracker ) ,
			StatementCNF.fromInfixString( "Enemy(x, America) => Hostile(x)", tracker ) ,
			StatementCNF.fromInfixString( "American(West)" , tracker ) ,
			StatementCNF.fromInfixString( "Enemy(Nono, America)" , tracker )
		};
		StatementCNF hypothesis = StatementCNF.fromInfixString( "Criminal(West)" , tracker );
		Assert.assertFalse( Resolver.proveHypothesis( tracker , hypothesis , kb ) );
	}
}
