package to.msn.wings.x3033092lastkadai01;

import android.app.Activity;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;

import java.io.IOException;
import java.io.InputStream;

//参考文献：Firebaseの追加方法について
//Android プロジェクトに Firebase を追加する
//(https://firebase.google.com/docs/android/setup?hl=ja)
public class TranslateActivity extends AppCompatActivity {
    //文字認識をして翻訳をしたテキストを表示する
    TextView textView;
    //英語から日本語への翻訳をする時に立つのフラグ
    boolean EtoJflg = true;
    //日本語から英語への翻訳をする時に立つのフラグ
    boolean JtoEflg = false;

    //画面の遷移に関してはMainActivityと同様
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);//activity_subをレイアウトにセットする
        Button bt_sl = findViewById(R.id.button);//画像選択をするためのボタンを設定する
        textView = findViewById(R.id.textView);//文字認識と翻訳の結果を表示するtext viewの設定
        bt_sl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();//画面を遷移するためのオブジェクト
                intent.setAction(Intent.ACTION_PICK);//オブジェクトの行動を定義
                                                      // （遷移先からデータを取得する）
                intent.setType("image/*");//取得するデータのタイプを指定(画像）
                //startActivityForResultを用いることで遷移先のデータを保持したまま元の画面に戻ることができる
                startActivityForResult(intent, 0);//指定したintentの情報と
                                                             // request code=0をonActivityResultに渡して
                                                            //画像の保存場所へ遷移を開始する
            }
        });
        //MainActivityに戻るためのボタン
        Button bt_back = findViewById(R.id.back_button);
        //そのボタンを押したときのイベント処理
        bt_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //画面の遷移を終了することで、MainActivityに戻る
            }
        });

        //翻訳する言語の指定をするためのラジオボタンの設定
        final RadioGroup rgroup = findViewById(R.id.Rgroup); //ラジオボタンのグループを指定
        //ラジオボタンが変更されたときのイベント処理
        rgroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioButton: //ラジオボタン1の場合(日本語訳)
                        JtoEflg = false; //英訳をするときのフラグを降ろす
                        EtoJflg = true; //日本語をするときのフラグを立てる
                        break;
                    case R.id.radioButton2: //ラジオボタン2の場合(英訳)
                        EtoJflg = false; //日本語をするときのフラグを降ろす
                        JtoEflg = true; //英訳をするときのフラグを立てる
                        break;
                }
            }
        });
    }

    //画像の読み込みと文字認識についてはMainActivityと同様に行う
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    //遷移先からデータを受け取って、そのデータを画像に変換し、その画像から文字認識を行うためのメソッド
    //参考文献：画像をフォルダから読み込んで文字認識をする方法について
    //FirebaseのMLkitで文字認識をいろいろ試してみた(https://www.techceed-inc.com/engineer_blog/3526/)
    protected void onActivityResult(int requestCode, final int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //画像の保存先へ移動した時の処理
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                //画像選択で得たURIをinput streamに紐づけるためのオブジェクト
                ContentResolver contentResolver = getContentResolver();
                //Bitmapを生成するためのオブジェクト
                InputStream inputStream = null;
                //受けとった画像データの向きから、画像の傾きを示す値
                //参考文献：画像表示について
                //Androidアプリ開発 Exif 画像のExif情報を取得する
                //(http://java-lang-programming.com/ja/articles/84)
                int orientation = ExifInterface.ORIENTATION_UNDEFINED;
                //orientationの値からその角度を0,90,180,270の4パターンで取得する値
                //この値をMatrixに代入することで画像を正しい向きで表示できるようにする
                int rotation;
                try {
                    //受け取ったURIをBitmapへ変換するためにinput streamに紐づける
                    inputStream = contentResolver.openInputStream(data.getData());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //input streamからBitmapを生成する
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                try {
                    //同じURIをinput streamにもう一度紐づける
                    //Bitmapの変換にinput streamを用いたら、input streamの情報が正しく得られなかったため、
                    //もう一度、URIの紐づけを行う
                    //このinput streamから画像の傾きの情報を取得する
                    inputStream = contentResolver.openInputStream(data.getData());
                    //画像ファイルのExifタグを読み取るオブジェクト
                    ExifInterface exifInterface = new ExifInterface(inputStream);
                    //ExifInterfaceによって、画像の傾きを取得
                    orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //ログでorientationの値を確認する
                //正しい画像情報を取得できているかの確認も込み
                Log.d("Exif", "orientation" + orientation);
                //orientationの値によってrotationの値を決定する
                switch (orientation) {
                    //画像が90度傾いている場合
                    case ExifInterface.ORIENTATION_ROTATE_90:
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        rotation = 90;
                        break;
                    //画像が180度傾いている場合
                    case ExifInterface.ORIENTATION_ROTATE_180:
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        rotation = 180;
                        break;
                    //画像が270度傾いている場合
                    case ExifInterface.ORIENTATION_ROTATE_270:
                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        rotation = 270;
                        break;
                    //正常であれば、傾き0
                    default:
                        rotation = 0;
                }
                //ログで傾きの角度を確認する
                Log.d("Exif", "rotation" + rotation);
                //Bitmapの傾きを変更するためのオブジェクト
                Matrix transformMatrix = new Matrix();
                //rotationの値をmatrixに設定する
                transformMatrix.setRotate(rotation);
                try {
                    //matrixを引数で渡して傾きが修正されたBitmapを生成する
                    Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), transformMatrix, true);
                    //bitmapが傾いている場合
                    // 元のbitmapと新しく生成したbitmapを比較
                    if (!bitmap.sameAs(converted)) {
                        bitmap = converted;
                    }
                } catch (OutOfMemoryError error) {
                    //bitmapの変更ができていなかったら、エラーを通知する
                    Log.e("", "transformBitmap: ", error);
                }
                //image viewを設定する
                ImageView imageView = findViewById(R.id.imageView);
                //image viewにbitmapを表示
                imageView.setImageBitmap(bitmap);
                try {
                    //input streamの値をリセット
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //ここからMLKitの処理を追記
                //文字認識のための画像をbitmapから取得する
                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
                //画像から文字認識を行うオブジェクトを生成
                FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance()
                        .getCloudDocumentTextRecognizer();
                //imageから文字認識を行う
                detector.processImage(image)
                        //文字認識が成功した場合
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
                            @Override
                            public void onSuccess(FirebaseVisionDocumentText result) {
                                //認識したテキストをString型で取得する
                                String resultText = result.getText();
                                //認識したテキストをテキスト翻訳のメソッドに引数として渡す
                                translate_text(resultText);
                                //正しく文字認識されていることをログで表示する
                                Log.d("text","Recognition");
                            }
                        })
                        //文字認識が失敗した場合
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //ログで文字認識できてないことを通知する
                                Log.d("Error","Not Recognition");
                            }
                        });
            }
        }
    }

    //認識した文字を翻訳するためのメソッド
    //参考文献：翻訳について
    //MLkitを使用してテキストを翻訳する
    //(https://firebase.google.com/docs/ml-kit/android/translate-text?hl=ja)
    private void translate_text(String resultText){
        //日本語訳をする場合
        if(EtoJflg == true) {
            //翻訳機を生成するためのオプションの設定
            //ソース言語を英語に設定し、ターゲット言語を日本語に設定する
            FirebaseTranslatorOptions options_EtoJ =
                    new FirebaseTranslatorOptions.Builder()
                            .setSourceLanguage(FirebaseTranslateLanguage.EN)
                            .setTargetLanguage(FirebaseTranslateLanguage.JA)
                            .build();
            //オプションを引数にして、英語から日本語に翻訳する翻訳機を生成する
            final FirebaseTranslator englishJapaneseTranslator =
                    FirebaseNaturalLanguage.getInstance().getTranslator(options_EtoJ);
            //英語から日本語に翻訳する翻訳機のモデルをfirebaseでアプリにダウンロードする
            FirebaseModelDownloadConditions conditions_EtoJ = new FirebaseModelDownloadConditions.Builder()
                    .requireWifi()
                    .build();
            englishJapaneseTranslator.downloadModelIfNeeded(conditions_EtoJ)
                    //ダウンロードが成功した時の処理
                    .addOnSuccessListener(
                            new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void v) {
                                    //ログでダウンロードが成功したことを通知する
                                    Log.d("Success", "Download");
                                }
                            })
                    //ダウンロードが失敗した時の処理
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //ログでダウンロードが失敗したことを通知する
                                    Log.d("Error", "Not Download");
                                }
                            });
            //生成した翻訳機に文字認識をしたテキストを引数で渡して翻訳をする
            englishJapaneseTranslator.translate(resultText)
                    //翻訳が成功した時の処理
                    .addOnSuccessListener(
                            new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(@NonNull String translatedText) {
                                    //翻訳したテキストをtext viewに表示する
                                    textView.setText(translatedText);
                                    //ログで翻訳したテキストを出力して、翻訳が成功したことを通知する
                                    Log.d("translated text", translatedText);
                                }
                            })
                    //翻訳が失敗した時の処理
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //ログで失敗したことを通知する
                                    Log.d("Error", "Not Translate");
                                }
                            });
        }
        //英訳をする場合
        //日本語訳とほぼ同様
        else if (JtoEflg == true){
            //翻訳機を生成するためのオプションの設定
            //ソース言語を日本語に設定し、ターゲット言語を英語に設定する
            FirebaseTranslatorOptions options_JtoE =
                    new FirebaseTranslatorOptions.Builder()
                            .setSourceLanguage(FirebaseTranslateLanguage.JA)
                            .setTargetLanguage(FirebaseTranslateLanguage.EN)
                            .build();
            //オプションを引数にして、英語から日本語に翻訳する翻訳機を生成する
            final FirebaseTranslator japaneseEnglishTranslator =
                    FirebaseNaturalLanguage.getInstance().getTranslator(options_JtoE);
            //日本語から英語に翻訳する翻訳機のモデルをfirebaseでアプリにダウンロードする
            FirebaseModelDownloadConditions conditions_JtoE = new FirebaseModelDownloadConditions.Builder()
                    .requireWifi()
                    .build();
            japaneseEnglishTranslator.downloadModelIfNeeded(conditions_JtoE)
                    //ダウンロードが成功した時の処理
                    .addOnSuccessListener(
                            new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void v) {
                                    //ログでダウンロードが成功したことを通知する
                                    Log.d("Success", "Download");
                                }
                            })
                    //ダウンロードが失敗した時の処理
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //ログでダウンロードが失敗したことを通知する
                                    Log.d("Error", "Not Download");
                                }
                            });
            //生成した翻訳機に文字認識をしたテキストを引数で渡して翻訳をする
            japaneseEnglishTranslator.translate(resultText)
                    //翻訳が成功した時の処理
                    .addOnSuccessListener(
                            new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(@NonNull String translatedText) {
                                    //翻訳したテキストをtext viewに表示する
                                    textView.setText(translatedText);
                                    //ログで翻訳したテキストを出力して、翻訳が成功したことを通知する
                                    Log.d("translated text", translatedText);
                                }
                            })
                    //翻訳が失敗した時の処理
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //ログで失敗したことを通知する
                                    Log.d("Error", "Not Translate");
                                }
                            });
        }
    }
}