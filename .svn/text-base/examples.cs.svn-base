new t2dSceneGraph(sceneGraph) {
	field = "ignored";
};

// Try starting to type sce, then pause for half a second
// A window showing sceneGraph should pop up.  You can use the arrows
// to select a suggestion, then press enter to insert it.  Or you
// can press esc to get rid of the window.

$global = new t2dStaticSprite(anotherGlobal);
// Now $global and anotherGlobal are defined as t2dStaticSprites.
// Type "$global."  That means type $global followed by a period, dot
// tochka, punto, or full stop (depending on where you're from).
// All of the member functions and variables related to 
// t2dStaticSprite pop up for you to choose from.

%object = new t2dSceneObject() {
	class = "myparent";
	superclass = "grandparent";
};

function grandparent::(%this, %unknownParameter) {

	// When you start a comment with //# that means a special command follows.  
	// You can comment out the special comment and the parser will ignore it.  
	// That is, you can type ///# or ////# and the parser will treat it as 
	// a normal comment.
	
	// The particular special comment below defines the class type of the
	// specified variable.  The first line is like saying:
	// "The variable %unknownParameter is a t2dParticleEffect"
	
	//# %unknownParameter t2dParticleEffect
	//# $unknownGlobal myparent
	//# $unknownGlobal.testfield SimGroup
	
	// This is necessary because torqueScript is a typeless language.
	// I extract the class/type whenever possible, but sometimes I
	// can't get it, or it just misses it or something.  So you can
	// define it explicitly when you need to.
	
	// The implementation of the special comment adds the class to a list of parents,
	// so you can specify several parents.  Keep in mind that this
	// does absolutely NOTHING to do with torque or tgb, it will not
	// change the way your code works.  This is only for code completion
	// purposes.  
	
	// So, try it out.  Below the comments, start typing
	// one of the variables defined (like the first 2 letters), 
	// pause for half a second, then select an item from the list.
	// The bottom example is especially cool... try typing a . at
	// the end of the defined variable.
	

}

function myparent::lookForMe(%this, %second) {
	// t2dVector is NOT actually a console class.
	// It is something I made up to help with code completion
	// The only way to use it right now is to use the
	// special comment //# to define it
	//# %second t2dVector
	
	
	// Now try typing %second. and see what pops up.
	// Then select one of the options.  Neat huh?
}