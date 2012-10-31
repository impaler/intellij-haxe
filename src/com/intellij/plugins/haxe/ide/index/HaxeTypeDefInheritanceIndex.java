package com.intellij.plugins.haxe.ide.index;

import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeAnonymousType;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeExtends;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeOrAnonymous;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeTypeDefInheritanceIndex extends FileBasedIndexExtension<String, List<HaxeClassInfo>> {
  public static final ID<String, List<HaxeClassInfo>> HAXE_TYPEDEF_INHERITANCE_INDEX = ID.create("HaxeTypeDefInheritanceIndex");
  private static final int INDEX_VERSION = 0;
  private final DataIndexer<String, List<HaxeClassInfo>, FileContent> myIndexer = new MyDataIndexer();
  private final DataExternalizer<List<HaxeClassInfo>> myExternalizer = new HaxeClassInfoListExternalizer();

  @NotNull
  @Override
  public ID<String, List<HaxeClassInfo>> getName() {
    return HAXE_TYPEDEF_INHERITANCE_INDEX;
  }

  @Override
  public int getVersion() {
    return INDEX_VERSION;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return new EnumeratorStringDescriptor();
  }

  @Override
  public DataExternalizer<List<HaxeClassInfo>> getValueExternalizer() {
    return myExternalizer;
  }

  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return HaxeSdkInputFilter.INSTANCE;
  }

  @NotNull
  @Override
  public DataIndexer<String, List<HaxeClassInfo>, FileContent> getIndexer() {
    return myIndexer;
  }

  private static class MyDataIndexer implements DataIndexer<String, List<HaxeClassInfo>, FileContent> {
    @Override
    @NotNull
    public Map<String, List<HaxeClassInfo>> map(final FileContent inputData) {
      final PsiFile psiFile = inputData.getPsiFile();
      final PsiElement[] fileChildren = psiFile.getChildren();
      final List<AbstractHaxeTypeDefImpl> classes = ContainerUtil.map(ContainerUtil.filter(fileChildren, new Condition<PsiElement>() {
        @Override
        public boolean value(PsiElement element) {
          return element instanceof AbstractHaxeTypeDefImpl;
        }
      }), new Function<PsiElement, AbstractHaxeTypeDefImpl>() {
        @Override
        public AbstractHaxeTypeDefImpl fun(PsiElement element) {
          return (AbstractHaxeTypeDefImpl)element;
        }
      });
      if (classes.isEmpty()) {
        return Collections.emptyMap();
      }
      final Map<String, List<HaxeClassInfo>> result = new THashMap<String, List<HaxeClassInfo>>(classes.size());
      final Map<String, String> qNameCache = new THashMap<String, String>();
      for (AbstractHaxeTypeDefImpl haxeTypeDef : classes) {
        final HaxeClassInfo value = new HaxeClassInfo(haxeTypeDef.getQualifiedName(), HaxeComponentType.typeOf(haxeTypeDef));
        final HaxeTypeOrAnonymous haxeTypeOrAnonymous = haxeTypeDef.getTypeOrAnonymous();
        final HaxeType type = haxeTypeOrAnonymous == null ? null : haxeTypeOrAnonymous.getType();
        final HaxeAnonymousType anonymousType = haxeTypeOrAnonymous == null ? null : haxeTypeOrAnonymous.getAnonymousType();
        if (anonymousType != null) {
          final HaxeTypeExtends typeExtends = anonymousType.getAnonymousTypeBody().getTypeExtends();
          if (typeExtends != null) {
            final String classNameCandidate = typeExtends.getType().getText();
            final String key = classNameCandidate.indexOf('.') != -1 ?
                               classNameCandidate :
                               getQNameAndCache(qNameCache, fileChildren, classNameCandidate);
            put(result, key, value);
          }
        }
        else if (type != null) {
          final String classNameCandidate = type.getText();
          final String qName = classNameCandidate.indexOf('.') != -1 ?
                               classNameCandidate :
                               getQNameAndCache(qNameCache, fileChildren, classNameCandidate);
          put(result, qName, value);
        }
      }
      return result;
    }

    private static String getQNameAndCache(Map<String, String> qNameCache, PsiElement[] fileChildren, String classNameCandidate) {
      String result = qNameCache.get(classNameCandidate);
      if (result == null) {
        result = HaxeResolveUtil.getQName(fileChildren, classNameCandidate, true);
        qNameCache.put(classNameCandidate, result);
      }
      return result;
    }

    private static void put(Map<String, List<HaxeClassInfo>> map, String key, HaxeClassInfo value) {
      List<HaxeClassInfo> infos = map.get(key);
      if (infos == null) {
        infos = new ArrayList<HaxeClassInfo>();
        map.put(key, infos);
      }
      infos.add(value);
    }
  }
}