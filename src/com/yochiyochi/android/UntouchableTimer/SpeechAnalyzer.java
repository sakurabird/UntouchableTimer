/* 
 * 音声認識結果の文字列リストから、望ましい秒数値を取得するクラス
 * by Office JAM (Jo)
 * rev.1 : 初版 (2011-06-23)
 * rev.2 : 漢数字が入ってきたときにもうまく解析する (????-??-??) 
 * rev.3 : 似たような処理を繰り返しているので最適化し、さらに最終リリース向けにメソッドの private 化、デバッグ用メソッド削除 (????-??-??)
 * 使うときは、package名を変えてね
 */
package com.yochiyochi.android.UntouchableTimer;


import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpeechAnalyzer
{
	public static final int USE_HOUR_IF_NO_UNIT = 3600;	// 文字列が数値のみの(単位に該当する箇所が無かった)場合、数値を「時間」とみなす
	public static final int USE_MUNITE_IF_NO_UNIT = 60;	// 文字列が数値のみの(単位に該当する箇所が無かった)場合、数値を「分」とみなす
	public static final int USE_SECOND_IF_NO_UNIT = 1;	// 文字列が数値のみの(単位に該当する箇所が無かった)場合、数値を「秒」とみなす
	private static final String regexNum = "[0-9]+";	// 数字列を表現する正規表現。「03分」も救ってあげる
	private static final String regexExactHMS[] = { "時間", "分", "秒" };	// 正しい音声認識の場合の時間、分、秒
	private static final String regexHMS[] = { regexExactHMS[0]+"|じかん|ジカン", regexExactHMS[1]+"|ふん|ぷん|フン|プン", regexExactHMS[2]+"|びょう|ビョウ" };	// 時間、分、秒の音声誤認識となりそうなものをどんどん追加

	public static final int COMMAND_NONE = -1;	// 音声コマンドに該当しない場合
	public static final int COMMAND_CANCEL = 0;	// 音声コマンド「中止」
	public static final int COMMAND_STOP = 1;	// 音声コマンド「停止」
	public static final int COMMAND_START = 2;	// 音声コマンド「開始」

	// 音声認識結果の文字列リストからコマンドを取得する
	public static int speechToCommand(ArrayList<String> strList)
	{
		final String regCommand[] = {
				"中止|中断|戻る|キャンセル|キャンプ|cancel|だめ|もう1回|もいっかい",
				"停止|定期|天使|ac|終了|資料|終わり|止まれ|やめて|うるさい|うざい|ストップ|スタバ|ラップ|トップ|stop|dap",
				"開始|回避|始め|はじめ|多治見|アニメ|スタート|スター|start|ゴー|ごお|5"
			};	// 順に、コマンド「中止」「停止」「開始」に該当する文言

		for(int i = 0; i < strList.size(); i++)		// 音声認識の第一候補から順に
			for(int j = 0; j < regCommand.length; j++)		// 「中止」「停止」「開始」の順に
				if(Pattern.compile(regCommand[j]).matcher(strList.get(i)).find())
					return j;
		return COMMAND_NONE;
	}

	// 音声認識結果の文字列リストから最適な秒数値を取得する
	public static int speechToSecond(ArrayList<String> strList)
	{
		return getSecond(getCandidate(strList), USE_MUNITE_IF_NO_UNIT);	// 文字列が数値のみの場合の単位の指定が無かった場合は、デフォルトとして「分」を使う
	}

	// 音声認識結果の文字列リストから最適な秒数値を取得する (デフォルトの単位を指定する場合)
	public static int speechToSecond(ArrayList<String> strList, int unit)
	{
		return getSecond(getCandidate(strList), unit);
	}
	
	// 文字列から秒数値を取得する
	// 本当は private でよいが、TestTextToSecond サンプルアプリではキー入力の文字列を秒数値に変換しているので、呼び出し可能にしている
	private static int getSecond(String str, int unit)
	{
    	Matcher mch;

    	str = kanjiToAlabic(str);	// 漢数字、全角数字を変換する
	    int result = 0;
    	if(valueOf(str) < 0)		// 文字列が数字のみ「ではない」場合、時間、分、秒を解析して、秒数値を返す
    	{
        	for(int i = 0; i < 3; i++)	// 時間、分、秒の順に処理する
        	{
        		result *= 60;
        		mch = Pattern.compile("("+regexNum+")("+regexHMS[i]+")").matcher(str);
        		if(mch.find())
        			result += Integer.valueOf(mch.group(1));
        	}
        	if(result == 0)		// この段階で結果が0だったら、第4優先に該当するはずなので、文字列に含まれる数値を全部足し、unitで指定される単位とみなす
        	{
        		mch = Pattern.compile(regexNum).matcher(str);
        		while(mch.find())
        			result += Integer.valueOf(mch.group())*unit;
        	}
    	}
    	else 			   		// 文字列が数字のみの場合、その数値が unit で指定される単位であるとみなして、秒数値を返す
    	{
			mch = Pattern.compile(regexNum).matcher(str);
    		if(mch.find())
    			result = Integer.valueOf(mch.group())*unit;
    	}
    	return result;
	}

	// 文字列リストの中から、時間情報として適切なものを取得する
	// 本当は private でよいが、TestTextToSecond サンプルアプリでは時間情報を取得した文字列を表示しているので、呼び出し可能にしている
	private static String getCandidate(ArrayList<String> strList)
	{
		// 漢数字、全角数字置換のため作業用文字列リストを作成
		ArrayList<String> tmpList = new ArrayList<String>();
		for(int i = 0; i < strList.size(); i++)
			tmpList.add(kanjiToAlabic(strList.get(i)));
		// 第1優先 : 文字列がもろに時間情報にマッチしている場合
		for(int i = 0; i < tmpList.size(); i++)
			if(isExactHMS(tmpList.get(i)))
				return strList.get(i);
		// 第2優先 : 多少の誤認識を許容して、時間情報らしきものにマッチした場合
		for(int i = 0; i < tmpList.size(); i++)
			if(isHMS(tmpList.get(i)))
				return strList.get(i);
		// 第3優先 : 文字列が数値のみで構成されている場合
		for(int i = 0; i < tmpList.size(); i++)
			if(valueOf(tmpList.get(i)) > 0)
				return strList.get(i);
		// 第4優先 : 文字列が数値を含む場合
		for(int i = 0; i < tmpList.size(); i++)
			if(isContainNum(tmpList.get(i)))
				return strList.get(i);
		// 最低優先 : しょうがないから、音声認識で最もスコアの高いものを返す。でもこの文字列からは時間情報を取れないなぁ。
		return strList.get(0);
	}

/* このメソッドは、デバッグ用です。どの優先順の候補が選択されたのかを確認するために。最終的には削除します。
	public static String getCandidate(ArrayList<String> strList, boolean debug)
	{
		// 漢数字、全角数字置換のため作業用文字列リストを作成
		ArrayList<String> tmpList = new ArrayList<String>();
		for(int i = 0; i < strList.size(); i++)
			tmpList.add(kanjiToAlabic(strList.get(i)));
		// 第1優先 : 文字列がもろに時間情報にマッチしている場合
		for(int i = 0; i < tmpList.size(); i++)
			if(isExactHMS(tmpList.get(i)))
				return strList.get(i) + " (第1優先)";
		// 第2優先 : 多少の誤認識を許容して、時間情報らしきものにマッチした場合
		for(int i = 0; i < tmpList.size(); i++)
			if(isHMS(tmpList.get(i)))
				return strList.get(i) + " (第2優先)";
		// 第3優先 : 文字列が数値のみで構成されている場合
		for(int i = 0; i < tmpList.size(); i++)
			if(valueOf(tmpList.get(i)) > 0)
				return strList.get(i) + " (第3優先)";
		// 第4優先 : 文字列が数値を含む場合
		for(int i = 0; i < tmpList.size(); i++)
			if(isContainNum(tmpList.get(i)))
				return strList.get(i) + " (第4優先)";
		// 最低優先 : しょうがないから、音声認識で最もスコアの高いものを返す。でもこの文字列からは時間情報を取れないなぁ。
		return strList.get(0) + " (最低優先)";
	}
*/
	
	// 文字列がもろに時間情報にマッチしたら true を返す
	private static boolean isExactHMS(String str)
	{
		if(str != null && !str.equals(""))
			if(Pattern.compile("^\\s*("+regexNum+"\\s*"+regexExactHMS[0]+")?\\s*("+regexNum+"\\s*"+regexExactHMS[1]+")?\\s*("+regexNum+"\\s*"+regexExactHMS[2]+")?\\s*$").matcher(str).find())
				return true;
		return false;
	}

	// 多少誤認識があるが、時間情報らしきものがあれば true を返す
	private static boolean isHMS(String str)
	{
		if(str != null && !str.equals(""))
			if(Pattern.compile(regexNum+regexHMS[0]).matcher(str).find())
				return true;
			else if(Pattern.compile(regexNum+regexHMS[1]).matcher(str).find())
				return true;
			else if(Pattern.compile(regexNum+regexHMS[2]).matcher(str).find())
				return true;
		return false;
	}
	
	// 文字列が数値のみで構成されていたら、その値 を返す、数値のみでなければ -1 を返す
	private static int valueOf(String str)
	{
		if(str != null && !str.equals(""))
		{
			Matcher mch = Pattern.compile("^\\s*"+regexNum+"\\s*$").matcher(str);
			if(mch.find())
				return Integer.valueOf(mch.group());
		}
		return (-1);
	}
	
	// 文字列に数値を含むなら true を返す
	private static boolean isContainNum(String str)
	{
		if(str != null && !str.equals(""))
			if(Pattern.compile(regexNum).matcher(str).find())
				return true;
		return false;
	}
	
	// 漢数字や全角数字を半角数字に変換した結果を返す
	// 本当は private でよい。デバッグ用に public
	private static String kanjiToAlabic(String str)
	{
		final String regexKanNum[] = { "零|０", "一|１", "二|２", "三|３", "四|４", "五|５", "六|６", "七|７", "八|８", "九|９" };
		final String regexKanUnit[] = { "", "十", "百", "千", "万" };	// 桁を表す文字。まあ、万まであればいいでしょ。ただし1文字だけね。
		Matcher mch, mchN;
		StringBuffer strBuf = new StringBuffer();
		
		if(str != null && !str.equals(""))
		{
			// まず単純に数字を半角数字にする
			for(int i = 0; i < regexKanNum.length; i++)
				str = str.replaceAll(regexKanNum[i], Integer.valueOf(i).toString());
			// 数字の右に単位が付いていないものは、一の位と考えて「/数値/」という文字列に置き換える
			strBuf.setLength(0);
			mch = Pattern.compile("("+regexNum+")([^[0-9]"+regexKanUnit[1]+regexKanUnit[2]+regexKanUnit[3]+regexKanUnit[4]+"])").matcher(str);
			while(mch.find())
				mch.appendReplacement(strBuf, "/"+mch.group(1)+"/"+mch.group(2));
			mch.appendTail(strBuf);
			str = strBuf.toString();
			// 数字の右に文字が無いものは、一の位と考えて「/数値/」という文字列に置き換える
			strBuf.setLength(0);
			mch = Pattern.compile("("+regexNum+")(\\s*)$").matcher(str);
			while(mch.find())
				mch.appendReplacement(strBuf, "/"+mch.group(1)+"/"+mch.group(2));
			mch.appendTail(strBuf);
			str = strBuf.toString();
			// 数字の右に単位が付いているものは、その単位の数値と考えて「/数値/」という文字列に置き換える
			for(int i = 1; i < regexKanUnit.length; i++)
			{
				strBuf.setLength(0);
				mch = Pattern.compile("("+regexNum+")"+regexKanUnit[i]).matcher(str);
				while(mch.find())
					mch.appendReplacement(strBuf, "/"+Integer.valueOf(Integer.valueOf(mch.group(1))*(int)Math.pow(10, i)).toString()+"/");
				mch.appendTail(strBuf);
				str = strBuf.toString();
			}
			// 残る「十」「百」「千」は、「(10)」「(100)」「(1000)」にする
			for(int i = 1; i < regexKanUnit.length-1; i++)
				str = str.replaceAll(regexKanUnit[i], "/"+Integer.valueOf((int)Math.pow(10, i)).toString()+"/");
			// 残る「万」は、「」(空)にする
			str = str.replaceAll(regexKanUnit[4], "");
			// 最後に数値を統合する
			strBuf.setLength(0);
			mch = Pattern.compile("(/"+regexNum+"/)+").matcher(str);
			while(mch.find())
			{
				int num = 0;
				mchN = Pattern.compile("/("+regexNum+")/").matcher(mch.group());
				while(mchN.find())
					num += Integer.valueOf(mchN.group(1));
				mch.appendReplacement(strBuf, Integer.valueOf(num).toString());
			}
			mch.appendTail(strBuf);
			str = strBuf.toString();
		}
		return str;
	}
}
